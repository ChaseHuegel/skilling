package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies the parse → evaluate pipeline keeps string-valued parameter
 * constants (e.g. {@code effect: { constant: "minecraft:poison" }}) as strings
 * instead of coercing them to {@code 0.0}, which previously made
 * {@code PotionEffectResolver} throw on every activation.
 */
class SkillEventListenerParamsTest {

    @Test
    void evaluateParamsEmitsRawStringForStringConstant() {
        var entry = new SkillDefinition.MechanicEntry(
                "core:apply_status",
                List.of(),
                Map.of("effect", new ConstantEvaluator("minecraft:poison"))
        );
        Map<String, Object> params = SkillEventListener.evaluateParams(entry, 10, 1);
        assertEquals("minecraft:poison", params.get("effect"));
        assertInstanceOf(String.class, params.get("effect"));
    }

    @Test
    void evaluateParamsKeepsNumericParamsAsDoubles() {
        var entry = new SkillDefinition.MechanicEntry(
                "core:apply_status",
                List.of(),
                Map.of("duration", new ConstantEvaluator(3.0))
        );
        Map<String, Object> params = SkillEventListener.evaluateParams(entry, 10, 1);
        assertEquals(3.0, params.get("duration"));
        assertInstanceOf(Double.class, params.get("duration"));
    }
}
