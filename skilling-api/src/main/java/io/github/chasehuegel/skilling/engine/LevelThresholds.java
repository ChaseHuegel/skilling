package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Precomputes and caches a skill's level thresholds so {@link SkillDefinition#getLevelForXp}
 * never re-evaluates the XP curve on the event path.
 *
 * <p>A threshold is the truncated {@code (long)} requirement of each level (1..maxLevel)
 * for a given evaluator, with non-finite requirements marked unreachable. Tables are keyed
 * by the evaluator instance and {@code maxLevel}, and cached weakly: while a loaded
 * {@code SkillDefinition} keeps its evaluator alive the table is reused, and a reload that
 * replaces evaluators lets old entries be collected. This satisfies "invalidated on reload"
 * without a manual cache clear.
 *
 * <p>Tables also record whether the truncated thresholds are non-decreasing, which lets
 * {@code getLevelForXp} binary-search the common monotonic curves while falling back to a
 * linear scan for arbitrary non-monotonic evaluators so behavior is preserved exactly.
 */
final class LevelThresholds {

    /** A level's thresholds plus whether the failure predicate is monotonic. */
    record Table(long[] thresholds, boolean[] unreachable, boolean sorted) {}

    private static final Map<ParameterEvaluator, Map<Integer, Table>> CACHE = new WeakHashMap<>();
    private static final Object LOCK = new Object();

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
        synchronized (LOCK) {
            Map<Integer, Table> byLevel = CACHE.computeIfAbsent(evaluator, k -> new HashMap<>());
            Table cached = byLevel.get(maxLevel);
            if (cached != null) return cached;
            Table computed = compute(evaluator, maxLevel);
            byLevel.put(maxLevel, computed);
            return computed;
        }
    }

    private static Table compute(ParameterEvaluator evaluator, int maxLevel) {
        long[] thresholds = new long[maxLevel];
        boolean[] unreachable = new boolean[maxLevel];
        boolean sorted = true;
        long prev = Long.MIN_VALUE;
        for (int level = 1; level <= maxLevel; level++) {
            double required = evaluator.evaluate(level, 0);
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
}
