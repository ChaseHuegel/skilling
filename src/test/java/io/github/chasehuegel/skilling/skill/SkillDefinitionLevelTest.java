package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
