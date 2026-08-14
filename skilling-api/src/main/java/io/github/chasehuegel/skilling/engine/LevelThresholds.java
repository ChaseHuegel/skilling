package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Precomputes and caches a skill's level thresholds so {@link SkillDefinition#getLevelForXp}
 * never re-evaluates the XP curve on the event path.
 *
 * <p>A threshold is the truncated {@code (long)} requirement of each level (1..maxLevel)
 * for a given evaluator, with non-finite requirements marked unreachable. Thresholds are
 * evaluated anchored at level 1 ({@code evaluate(level, 1)}), so {@code base_xp} is the
 * exact requirement for level 1 across all progression curves (e.g. a {@code linear} curve
 * registered with base {@code base_xp} and step {@code base_xp * 0.1} requires
 * {@code base_xp} at level 1). Tables are keyed by the evaluator instance and
 * {@code maxLevel}, and the keys are held weakly: while a loaded {@code SkillDefinition}
 * keeps its evaluator alive the table is reused, and a reload that replaces evaluators
 * lets old entries be collected. This satisfies "invalidated on reload" without a manual
 * cache clear.
 *
 * <p>Tables also record whether the truncated thresholds are non-decreasing, which lets
 * {@code getLevelForXp} binary-search the common monotonic curves while falling back to a
 * linear scan for arbitrary non-monotonic evaluators so behavior is preserved exactly.
 *
 * <p>The cache is lock-free: lookups and computations are per-evaluator, so distinct
 * skills (and distinct max levels within a skill) never serialize on a global lock.
 */
final class LevelThresholds {

    /** A level's thresholds plus whether the failure predicate is monotonic. */
    record Table(long[] thresholds, boolean[] unreachable, boolean sorted) {}

    /**
     * Weak-keyed cache of threshold tables. The outer map is keyed by a {@link Key}
     * that holds its evaluator in a {@link WeakReference}, preserving the old
     * {@code WeakHashMap} reload behavior; the inner per-evaluator map is a
     * {@link ConcurrentHashMap} so distinct skills and levels compute in parallel.
     */
    static final ConcurrentMap<Key, ConcurrentMap<Integer, Table>> CACHE = new ConcurrentHashMap<>();

    private LevelThresholds() {}

    /**
     * Returns (computing once if absent) the threshold table for the given evaluator and
     * maximum level.
     *
     * @param evaluator the skill's progression evaluator
     * @param maxLevel  the skill's maximum level
     * @return the cached threshold table
     */
    static Table table(ParameterEvaluator evaluator, int maxLevel) {
        Key key = new Key(evaluator);
        // Drop keys whose evaluator was garbage-collected (after a reload) so the
        // weak cache cannot grow without bound. This runs OUTSIDE the mapping
        // function below: the CHM contract forbids updating a map from its own
        // computeIfAbsent mapping function (it can livelock or corrupt). The scan
        // is O(distinct live evaluators) — bounded by the number of loaded skills
        // — so it stays cheap even on the hot getLevelForXp path.
        CACHE.keySet().removeIf(Key::cleared);
        ConcurrentMap<Integer, Table> byLevel = CACHE.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        // compute() is atomic per (evaluator, maxLevel): the mapping function runs
        // at most once per key, and unrelated keys never wait on it.
        return byLevel.compute(maxLevel, (level, existing) ->
                existing != null ? existing : compute(evaluator, maxLevel));
    }

    private static Table compute(ParameterEvaluator evaluator, int maxLevel) {
        long[] thresholds = new long[maxLevel];
        boolean[] unreachable = new boolean[maxLevel];
        boolean sorted = true;
        long prev = Long.MIN_VALUE;
        for (int level = 1; level <= maxLevel; level++) {
            // Progression thresholds are anchored at level 1 so base_xp is the
            // exact level-1 requirement (matching the polynomial curve).
            double required = evaluator.evaluate(level, 1);
            int idx = level - 1;
            if (!Double.isFinite(required)) {
                // A non-finite requirement is unreachable at any XP, mirroring the
                // linear scan's short-circuit; the sentinel keeps the array non-decreasing.
                unreachable[idx] = true;
                thresholds[idx] = Long.MAX_VALUE;
            } else {
                thresholds[idx] = (long) required;
            }
            if (thresholds[idx] < prev) sorted = false;
            prev = thresholds[idx];
        }
        return new Table(thresholds, unreachable, sorted);
    }

    /**
     * Cache key wrapping the evaluator in a {@link WeakReference}. Equality is by
     * evaluator identity, so a reload's fresh evaluator instance (even with identical
     * parameters) keys a new table, and {@link #cleared()} lets the cache reclaim
     * entries whose evaluator has been collected.
     */
    static final class Key {
        private final WeakReference<ParameterEvaluator> ref;
        private final int hash;

        Key(ParameterEvaluator evaluator) {
            this.ref = new WeakReference<>(evaluator);
            this.hash = System.identityHashCode(evaluator);
        }

        boolean cleared() {
            return ref.get() == null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key other)) return false;
            ParameterEvaluator a = ref.get();
            ParameterEvaluator b = other.ref.get();
            return a != null && a == b;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
