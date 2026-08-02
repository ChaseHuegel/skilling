package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillManagerCustomEvaluatorTest {

    /** A registered addon evaluator with a no-arg constructor. */
    public static class LogisticEvaluator implements ParameterEvaluator {
        public LogisticEvaluator() {}

        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            return 100.0 / (1.0 + Math.exp(-(currentLevel - unlockLevel)));
        }
    }

    private SkillManager skillManagerWithLogistic() {
        var evalReg = new EvaluatorRegistry();
        Skilling.registerBuiltinEvaluators(evalReg);
        evalReg.register("logistic", LogisticEvaluator.class);
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        return new SkillManager(evalReg, mechReg, new TriggerRegistry(),
                new TagResolver(new CustomTagLoader()));
    }

    private SkillManager skillManagerWithoutLogistic() {
        var evalReg = new EvaluatorRegistry();
        Skilling.registerBuiltinEvaluators(evalReg);
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        return new SkillManager(evalReg, mechReg, new TriggerRegistry(),
                new TagResolver(new CustomTagLoader()));
    }

    @Test
    void registeredCustomCurveParsesAndEvaluates() {
        var skillManager = skillManagerWithLogistic();
        var config = YamlConfiguration.loadConfiguration(new StringReader("""
                id: test
                max_level: 100
                progression: { curve: "logistic" }
                """));
        SkillDefinition def = skillManager.parseSkill(config);
        ParameterEvaluator evaluator = def.progression().evaluator();
        assertInstanceOf(LogisticEvaluator.class, evaluator);
        assertEquals(50.0, evaluator.evaluate(0, 0), 1e-6);
    }

    @Test
    void registeredCustomEvaluatorWorksAsParameterType() {
        var skillManager = skillManagerWithLogistic();
        var config = YamlConfiguration.loadConfiguration(new StringReader("""
                id: test
                max_level: 100
                progression: { curve: "constant", base_xp: 100 }
                abilities:
                  - id: a
                    unlock_level: 1
                    trigger: block_break
                    mechanics:
                      - type: core:yield_multiplier
                        parameters:
                          yield_chance: { logistic: { k: 1 } }
                    feedback: { notify: { action_bar: false } }
                """));
        SkillDefinition def = skillManager.parseSkill(config);
        ParameterEvaluator param = def.abilities().get(0).mechanics().get(0).parameters().get("yield_chance");
        assertInstanceOf(LogisticEvaluator.class, param);
    }

    @Test
    void unregisteredEvaluatorTypeStillThrows() {
        var skillManager = skillManagerWithoutLogistic();
        var config = YamlConfiguration.loadConfiguration(new StringReader("""
                id: test
                max_level: 100
                progression: { curve: "constant", base_xp: 100 }
                abilities:
                  - id: a
                    unlock_level: 1
                    trigger: block_break
                    mechanics:
                      - type: core:yield_multiplier
                        parameters:
                          yield_chance: { logistic: { k: 1 } }
                    feedback: { notify: { action_bar: false } }
                """));
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }
}
