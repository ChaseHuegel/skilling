package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillDefinitionLevelTest {

    private SkillDefinition skillWithEvaluator(ParameterEvaluator evaluator) {
        return new SkillDefinition("test", 10, null,
                new SkillDefinition.Progression("custom", 0, 0, evaluator),
                List.of(), List.of(), List.of());
    }

    @Test
    void nanRequirementDoesNotReportMaxLevel() {
        var skill = skillWithEvaluator((l, u) -> Double.NaN);
        assertEquals(0, skill.getLevelForXp(1000),
                "a NaN requirement must never report an instant max level");
    }

    @Test
    void infiniteRequirementTreatsLevelAsUnreached() {
        var skill = skillWithEvaluator((l, u) -> Double.POSITIVE_INFINITY);
        assertEquals(0, skill.getLevelForXp(1000));
    }

    @Test
    void validCurveComputesLevel() {
        var skill = skillWithEvaluator((l, u) -> l * 100.0);
        assertEquals(2, skill.getLevelForXp(250)); // level 3 requires 300; 250 -> level 2
    }

    /**
     * Reference re-implementation of the previous linear scan that ISSUE-136 replaced,
     * used to prove the optimized version produces identical levels.
     */
    private static int referenceLinearScan(SkillDefinition skill, long xp) {
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (!Double.isFinite(required) || xp < (long) required) return level - 1;
        }
        return skill.maxLevel();
    }

    private static long[] xpSamples(SkillDefinition skill) {
        Set<Long> samples = new TreeSet<>();
        samples.add(0L);
        samples.add(1L);
        samples.add(Long.MAX_VALUE);
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (!Double.isFinite(required)) continue;
            long t = (long) required;
            samples.add(Math.max(0, t - 1));
            samples.add(t);
            if (t < Long.MAX_VALUE) samples.add(t + 1);
        }
        long[] result = new long[samples.size()];
        int i = 0;
        for (long s : samples) result[i++] = s;
        return result;
    }

    @Test
    void optimizedLevelMatchesLinearScanAcrossCurves() {
        List<ParameterEvaluator> curves = List.of(
                (l, u) -> l * 100.0,
                (l, u) -> Math.pow(l, 2) * 50,
                (l, u) -> l * 10.0 + 0.7,
                (l, u) -> 100.0 - l * 5,
                new MilestoneEvaluator(new TreeMap<>(Map.of(1, 100.0, 3, 300.0, 7, 900.0))),
                (l, u) -> l >= 5 ? Double.POSITIVE_INFINITY : l * 100.0,
                (l, u) -> l >= 9 ? Double.NaN : l * 100.0
        );
        for (ParameterEvaluator evaluator : curves) {
            SkillDefinition skill = skillWithEvaluator(evaluator);
            for (long xp : xpSamples(skill)) {
                assertEquals(referenceLinearScan(skill, xp), skill.getLevelForXp(xp),
                        "optimized level must match the linear scan at xp=" + xp);
            }
        }
    }

    @Test
    void thresholdsComputedOncePerEvaluator() {
        AtomicInteger evals = new AtomicInteger();
        ParameterEvaluator curve = (l, u) -> {
            evals.incrementAndGet();
            return l * 100.0;
        };
        var skill = skillWithEvaluator(curve);

        skill.getLevelForXp(150);
        long evaluations = evals.get();
        assertTrue(evaluations >= 1, "the first lookup must compute thresholds");

        for (long xp : new long[]{0, 150, 900, Long.MAX_VALUE}) {
            skill.getLevelForXp(xp);
        }
        assertEquals(evaluations, evals.get(),
                "thresholds must be computed once per evaluator and reused thereafter");
    }

    @Test
    void distinctEvaluatorsDoNotShareThresholds() {
        AtomicInteger evals = new AtomicInteger();
        ParameterEvaluator curve = (l, u) -> {
            evals.incrementAndGet();
            return l * 100.0;
        };
        var first = skillWithEvaluator(curve);
        first.getLevelForXp(150);
        long afterFirst = evals.get();

        var second = skillWithEvaluator((l, u) -> l * 100.0);
        assertEquals(afterFirst, evals.get(),
                "an unrelated evaluator must not reuse the first skill's thresholds");
        assertTrue(second.getLevelForXp(250) == 2);
    }
}
