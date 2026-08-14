package io.github.chasehuegel.skilling.feedback;

import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackDebouncerTest {

    @Test
    void firstCallReturnsTrue() {
        var debouncer = new FeedbackDebouncer(500);
        assertTrue(debouncer.tryDebounce(UUID.randomUUID(), "test_ability"));
    }

    @Test
    void secondCallWithinIntervalReturnsFalse() {
        var debouncer = new FeedbackDebouncer(500);
        UUID uuid = UUID.randomUUID();

        assertTrue(debouncer.tryDebounce(uuid, "test_ability"));
        assertFalse(debouncer.tryDebounce(uuid, "test_ability"));
    }

    @Test
    void differentAbilitiesNotDebounced() {
        var debouncer = new FeedbackDebouncer(500);
        UUID uuid = UUID.randomUUID();

        assertTrue(debouncer.tryDebounce(uuid, "ability_1"));
        assertTrue(debouncer.tryDebounce(uuid, "ability_2"));
    }

    @Test
    void differentPlayersNotDebounced() {
        var debouncer = new FeedbackDebouncer(500);

        assertTrue(debouncer.tryDebounce(UUID.randomUUID(), "test"));
        assertTrue(debouncer.tryDebounce(UUID.randomUUID(), "test"));
    }

    @Test
    void clearRemovesPlayerState() {
        var debouncer = new FeedbackDebouncer(500);
        UUID uuid = UUID.randomUUID();

        assertTrue(debouncer.tryDebounce(uuid, "test"));
        assertFalse(debouncer.tryDebounce(uuid, "test"));

        debouncer.clear(uuid);

        // After clear, fresh feedback is allowed again for that player.
        assertTrue(debouncer.tryDebounce(uuid, "test"));
        assertFalse(debouncer.tryDebounce(uuid, "test"));
    }

    @Test
    void zeroIntervalAllowsAll() {
        var debouncer = new FeedbackDebouncer(0);
        UUID uuid = UUID.randomUUID();

        assertTrue(debouncer.tryDebounce(uuid, "test"));
        assertTrue(debouncer.tryDebounce(uuid, "test"));
    }

    @Test
    void setIntervalMsAppliesAtRuntime() {
        var debouncer = new FeedbackDebouncer(500);
        UUID uuid = UUID.randomUUID();

        assertTrue(debouncer.tryDebounce(uuid, "test"));
        assertFalse(debouncer.tryDebounce(uuid, "test"));

        debouncer.setIntervalMs(0);
        assertTrue(debouncer.tryDebounce(uuid, "test"),
                "a runtime interval change must take effect");
    }

    @Test
    void concurrentCallsEmitAtMostOncePerInterval() throws Exception {
        var debouncer = new FeedbackDebouncer(500);
        UUID uuid = UUID.randomUUID();
        int workers = 8;
        var gate = new java.util.concurrent.CountDownLatch(1);
        var emissions = new java.util.concurrent.atomic.AtomicInteger();
        var threads = java.util.stream.IntStream.range(0, workers)
                .mapToObj(i -> new Thread(() -> {
                    try {
                        gate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (debouncer.tryDebounce(uuid, "test")) emissions.incrementAndGet();
                }))
                .toList();
        threads.forEach(Thread::start);
        gate.countDown();
        for (Thread t : threads) t.join();

        assertEquals(1, emissions.get(), "exactly one concurrent caller may emit within the interval");
    }

    @Test
    void sharedAbilityIdKeepsIndependentFeedbackPerSkill() {
        var debouncer = new FeedbackDebouncer(500);
        UUID uuid = UUID.randomUUID();

        assertTrue(debouncer.tryDebounce(uuid, "skill_a", "haste"),
                "first skill feedback must proceed");
        assertFalse(debouncer.tryDebounce(uuid, "skill_a", "haste"),
                "the same skill's feedback must be debounced");

        // Another skill sharing the ability id must not be suppressed.
        assertTrue(debouncer.tryDebounce(uuid, "skill_b", "haste"),
                "a shared ability id in another skill must keep its own feedback");
    }
}