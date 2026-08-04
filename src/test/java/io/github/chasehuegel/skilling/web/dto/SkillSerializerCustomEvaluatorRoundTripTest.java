package io.github.chasehuegel.skilling.web.dto;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the web serializer preserves addon-registered custom evaluator types
 * (e.g. {@code { logistic: {...} }}) instead of collapsing them to a constant 0,
 * so a web GET/PUT round-trip does not rewrite the parameter.
 */
class SkillSerializerCustomEvaluatorRoundTripTest {

    /** A registered addon evaluator with a no-arg constructor. */
    public static class LogisticEvaluator implements ParameterEvaluator {
        public LogisticEvaluator() {}

        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            return 100.0 / (1.0 + Math.exp(-(currentLevel - unlockLevel)));
        }
    }

    private static SkillManager managerWithLogistic() {
        var evalReg = new EvaluatorRegistry();
        Skilling.registerBuiltinEvaluators(evalReg);
        evalReg.register("logistic", LogisticEvaluator.class);
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        var trigReg = new TriggerRegistry();
        Skilling.registerBuiltinTriggers(trigReg);
        return new SkillManager(evalReg, mechReg, trigReg,
                new TagResolver(new CustomTagLoader()), new StateFilterRegistry());
    }

    @Test
    void customEvaluatorTypeIsPreservedOnParse() {
        var dto = SkillSerializer.parseEvaluator(Map.of("logistic", Map.of("midpoint", 50)));
        assertEquals("logistic", dto.type());
        assertEquals(50.0, ((Number) dto.params().get("midpoint")).doubleValue());
    }

    @Test
    void customEvaluatorNotRewrittenOnSerialize() {
        var dto = new SkillDetailDTO.EvaluatorDTO("logistic", Map.of("midpoint", 50));
        assertEquals(Map.of("logistic", Map.of("midpoint", 50)), SkillSerializer.evaluatorToMap(dto));
    }

    @Test
    void customEvaluatorRoundTripsThroughWebAndEngine() {
        String yaml = """
                id: test
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                abilities:
                  - id: a
                    display_name: "A"
                    unlock_level: 1
                    trigger: "block_break"
                    mechanics:
                      - type: "core:yield_multiplier"
                        parameters:
                          yield_chance:
                            logistic: { midpoint: 50, steepness: 0.2 }
                    feedback: { notify: { action_bar: false } }
                """;

        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        var ev = dto.abilities().get(0).mechanics().get(0).parameters().get("yield_chance");
        assertEquals("logistic", ev.type(), "custom evaluator type must not collapse to a constant");

        // Web round-trip then engine parse keeps the type (registered evaluator).
        SkillDefinition def = managerWithLogistic().parseSkill(
                YamlConfiguration.loadConfiguration(new StringReader(SkillSerializer.toYaml(dto))));
        var evaluator = def.abilities().get(0).mechanics().get(0).parameters().get("yield_chance");
        assertInstanceOf(LogisticEvaluator.class, evaluator);
    }

    @Test
    void unknownMultiKeyEvaluatorIsRejectedLoudly() {
        // Not a valid evaluator shape: reject instead of silently zeroing.
        assertThrows(IllegalArgumentException.class,
                () -> SkillSerializer.parseEvaluator(Map.of("a", 1, "b", 2)));
    }
}
