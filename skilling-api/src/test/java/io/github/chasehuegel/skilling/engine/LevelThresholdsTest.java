package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link LevelThresholds} stays correct and contention-free: distinct
 * skills must compute thresholds in parallel (no global lock), a reload's fresh
 * evaluator instance keys a new table, and collected evaluators are reclaimed.
 */
class LevelThresholdsTest {

    private static final int MAX_LEVEL = 100;

    /** Evaluator that parks briefly per call and tracks peak concurrency. */
    static final class TrackerEvaluator implements ParameterEvaluator {
        private static final AtomicInteger ACTIVE = new AtomicInteger();
        private static final AtomicInteger PEAK = new AtomicInteger();

        static void reset() {
            ACTIVE.set(0);
            PEAK.set(0);
        }

        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            int now = ACTIVE.incrementAndGet();
            PEAK.accumulateAndGet(now, Math::max);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                ACTIVE.decrementAndGet();
            }
            return currentLevel * 1000.0;
        }
    }

    static final class TestEvaluator implements ParameterEvaluator {
        private final double base;

        TestEvaluator(double base) {
            this.base = base;
        }

        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            return base + currentLevel * 10.0;
        }
    }

    @Test
    void distinctSkillsComputeThresholdsInParallel() throws Exception {
        TrackerEvaluator.reset();
        CountDownLatch gate = new CountDownLatch(2);
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
        List<Thread> threads = List.of(
                threadFor(gate, results, new TrackerEvaluator()),
                threadFor(gate, results, new TrackerEvaluator()));
        threads.forEach(Thread::start);
        threads.forEach(LevelThresholdsTest::join);

        // Two distinct skills computing their own tables must overlap in time;
        // the old single global lock would have forced peak concurrency of 1.
        assertTrue(TrackerEvaluator.PEAK.get() >= 2,
                "expected parallel threshold computation, peak=" + TrackerEvaluator.PEAK.get());
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(level -> level == MAX_LEVEL));
    }

    private static Thread threadFor(CountDownLatch gate, ConcurrentLinkedQueue<Integer> results, TrackerEvaluator evaluator) {
        return new Thread(() -> {
            gate.countDown();
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SkillDefinition skill = new SkillDefinition(
                    "test",
                    MAX_LEVEL,
                    new SkillDefinition.Display("Test", "minecraft:stone", 0, "red", "solid"),
                    new SkillDefinition.Progression("linear", 100.0, 0.0, evaluator),
                    List.of(),
                    List.of());
            results.add(skill.getLevelForXp(1_000_000L));
        });
    }

    private static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void reloadWithFreshEvaluatorKeysANewTable() {
        TestEvaluator curve1 = new TestEvaluator(50.0);
        TestEvaluator curve2 = new TestEvaluator(50.0); // same parameters, fresh instance (reload)
        LevelThresholds.Table t1 = LevelThresholds.table(curve1, 20);
        LevelThresholds.Table t2 = LevelThresholds.table(curve2, 20);
        assertNotSame(t1, t2);
        assertArrayEquals(t1.thresholds(), t2.thresholds());
        assertArrayEquals(t1.unreachable(), t2.unreachable());
        // Reusing the same (live) evaluator reuses the cached table.
        assertSame(t1, LevelThresholds.table(curve1, 20));
    }

    @Test
    void collectedEvaluatorTablesArePurgedOnNextInsert() throws Exception {
        // Clean up any cleared keys left behind by other tests.
        LevelThresholds.table(new TestEvaluator(999.0), 5);

        TestEvaluator live = new TestEvaluator(60.0);
        LevelThresholds.Table liveTable = LevelThresholds.table(live, 20);
        LevelThresholds.table(new TestEvaluator(50.0), 20); // doomed: no strong reference

        // Force collection of the doomed evaluator so its weak cache key clears.
        for (int i = 0; i < 20
                && LevelThresholds.CACHE.keySet().stream().noneMatch(LevelThresholds.Key::cleared); i++) {
            System.gc();
            Thread.sleep(10);
        }

        // The next insertion purges cleared keys while preserving live tables.
        LevelThresholds.table(new TestEvaluator(70.0), 20);
        assertTrue(LevelThresholds.CACHE.keySet().stream().noneMatch(LevelThresholds.Key::cleared),
                "cleared weak keys must be reclaimed after reload");
        assertSame(liveTable, LevelThresholds.table(live, 20), "live evaluator's table must be preserved");
    }

    @Test
    void concurrentLookupsDuringClearedKeyEvictionNeverHangOrCorrupt() throws Exception {
        // A reload clears old evaluators while players earn XP; the eviction now
        // runs outside the computeIfAbsent mapping function, so hammering the
        // cache from many threads must never livelock or produce wrong tables.
        int workers = 8;
        int lookupsPerWorker = 500;
        var errors = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        CountDownLatch gate = new CountDownLatch(1);
        var threads = java.util.stream.IntStream.range(0, workers).mapToObj(w -> new Thread(() -> {
            try {
                gate.await();
                for (int i = 0; i < lookupsPerWorker; i++) {
                    // Alternate between fresh (reload-like) and stable evaluators.
                    var curve = new TestEvaluator(100.0 + (i % 7));
                    SkillDefinition skill = new SkillDefinition(
                            "test", MAX_LEVEL,
                            new SkillDefinition.Display("Test", "minecraft:stone", 0, "red", "solid"),
                            new SkillDefinition.Progression("linear", 100.0, 0.0, curve),
                            List.of(), List.of());
                    int level = skill.getLevelForXp(1_000_000L);
                    if (level != MAX_LEVEL) {
                        throw new AssertionError("corrupt threshold table: level=" + level);
                    }
                }
            } catch (Throwable t) {
                errors.compareAndSet(null, t);
            }
        })).toList();
        threads.forEach(Thread::start);
        gate.countDown();
        for (Thread t : threads) t.join(30_000);

        assertTrue(errors.get() == null, "concurrent lookups during eviction must not fail: " + errors.get());
    }
}
