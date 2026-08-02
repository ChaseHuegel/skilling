package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantValueEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class SkillManagerTest {

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
    }

    @Test
    void missingIdThrows() {
        var config = new YamlConfiguration();
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }

    @Test
    void blankIdThrows() {
        var config = new YamlConfiguration();
        config.set("id", "");
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }

    @Test
    void invalidMaxLevelThrows() {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("max_level", 0);
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }

    @Test
    void minimalSkillParses() {
        var config = new YamlConfiguration();
        config.set("id", "mining");
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);

        SkillDefinition def = skillManager.parseSkill(config);
        assertEquals("mining", def.id());
        assertEquals(100, def.maxLevel());
        assertNotNull(def.display());
        assertNotNull(def.progression());
        assertTrue(def.xpSources().isEmpty());
        assertTrue(def.abilities().isEmpty());
    }

    @Test
    void fullSkillParses() {
        String yaml = """
                id: "mining"
                max_level: 50
                display:
                  name: "Mining"
                  icon: "minecraft:iron_pickaxe"
                  custom_model_data: 1001
                  color: "GREEN"
                  style: "SEGMENTED_10"
                progression:
                  curve: "polynomial"
                  base_xp: 50
                  exponent: 2.5
                xp_sources:
                  - trigger: "block_break"
                    filters:
                      - target: "#c:ores"
                    reward:
                      constant: 15.0
                abilities:
                  - id: "geologist"
                    display_name: "Geologist"
                    unlock_level: 1
                    trigger: "block_break"
                    display:
                      lore:
                        - "Increases yield by {yield_chance}%."
                    requirements:
                      cooldown: 0
                    mechanics:
                      - type: "core:yield_multiplier"
                        parameters:
                          yield_chance:
                            linear:
                              base: 0.5
                              step: 0.5
                              max: 50.0
                    feedback:
                      notify:
                        action_bar: false
                        chat: false
                """;
        var config = YamlConfiguration.loadConfiguration(
                new java.io.StringReader(yaml)
        );
        SkillDefinition def = skillManager.parseSkill(config);
        assertEquals("mining", def.id());
        assertEquals(50, def.maxLevel());
        assertEquals("Mining", def.display().name());
        assertEquals("minecraft:iron_pickaxe", def.display().icon());
        assertEquals(1, def.xpSources().size());
        assertEquals("block_break", def.xpSources().get(0).trigger());
        assertEquals(1, def.abilities().size());
        assertEquals("geologist", def.abilities().get(0).id());
    }

    @Test
    void parseInlineEvaluatorConstant() {
        ParameterEvaluator eval = skillManager.parseInlineEvaluator(Map.of("constant", 15.0));
        assertInstanceOf(ConstantEvaluator.class, eval);
        assertEquals(15.0, eval.evaluate(1, 0));
    }

    @Test
    void parseInlineEvaluatorStringConstant() {
        ParameterEvaluator eval = skillManager.parseInlineEvaluator(Map.of("constant", "minecraft:poison"));
        assertInstanceOf(ConstantValueEvaluator.class, eval);
        assertEquals("minecraft:poison", ((ConstantValueEvaluator) eval).value());
    }

    @Test
    void parseInlineEvaluatorLinear() {
        ParameterEvaluator eval = skillManager.parseInlineEvaluator(Map.of(
                "linear", Map.of("base", 0.5, "step", 0.5, "max", 50.0)
        ));
        assertInstanceOf(LinearEvaluator.class, eval);
        assertEquals(0.5, eval.evaluate(1, 1));
        assertEquals(1.0, eval.evaluate(2, 1));
    }

    @Test
    void parseInlineEvaluatorMilestones() {
        ParameterEvaluator eval = skillManager.parseInlineEvaluator(Map.of(
                "milestones", Map.of("15", 3.0, "40", 8.0, "80", 16.0)
        ));
        assertInstanceOf(MilestoneEvaluator.class, eval);
        assertEquals(3.0, eval.evaluate(15, 1));
        assertEquals(8.0, eval.evaluate(40, 1));
        assertEquals(16.0, eval.evaluate(80, 1));
    }

    @Test
    void parseInlineEvaluatorPolynomial() {
        ParameterEvaluator eval = skillManager.parseInlineEvaluator(Map.of(
                "polynomial", Map.of("base_xp", 50.0, "exponent", 2.5)
        ));
        assertInstanceOf(PolynomialEvaluator.class, eval);
    }

    @Test
    void parseInlineEvaluatorUnknownThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                skillManager.parseInlineEvaluator(Map.of("unknown_type", Map.of()))
        );
    }

    @Test
    void parseInlineEvaluatorNullReturnsConstantZero() {
        ParameterEvaluator eval = skillManager.parseInlineEvaluator(null);
        assertInstanceOf(ConstantEvaluator.class, eval);
        assertEquals(0.0, eval.evaluate(1, 0));
    }

    private YamlConfiguration minimalSkill() {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        return config;
    }

    @Test
    void xpSourceMissingRewardThrows() {
        var config = minimalSkill();
        config.set("xp_sources", java.util.List.of(java.util.Map.of("trigger", "block_break")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("reward"));
    }

    @Test
    void nonMapXpSourceThrows() {
        var config = minimalSkill();
        config.set("xp_sources", java.util.List.of("not-a-map"));
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }

    @Test
    void stringCooldownThrows() {
        var config = minimalSkill();
        config.set("abilities", java.util.List.of(java.util.Map.of(
                "id", "a", "unlock_level", 1, "trigger", "block_break",
                "requirements", java.util.Map.of("cooldown", "5"))));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("Cooldown"));
    }

    @Test
    void stringUnlockLevelThrows() {
        var config = minimalSkill();
        config.set("abilities", java.util.List.of(java.util.Map.of(
                "id", "a", "unlock_level", "5", "trigger", "block_break")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("unlock_level"));
    }

    @Test
    void nonMapAbilityThrows() {
        var config = minimalSkill();
        config.set("abilities", java.util.List.of("not-a-map"));
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }

    @Test
    void unknownMechanicTypeThrows() {
        var config = minimalSkill();
        config.set("abilities", java.util.List.of(java.util.Map.of(
                "id", "a", "unlock_level", 1, "trigger", "block_break",
                "mechanics", java.util.List.of(java.util.Map.of("type", "core:nonexistent")))));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("Unknown mechanic"));
    }
}
