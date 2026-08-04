package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the required {@code trigger} field on abilities and the fail-fast
 * behaviour of {@link SkillManager} when it is missing or blank.
 */
class SkillManagerTriggerFieldTest {

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
    }

    @Test
    void missingTriggerThrows() {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        config.set("abilities", java.util.List.of(
                java.util.Map.of("id", "no_trigger", "unlock_level", 1)
        ));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("trigger"));
    }

    @Test
    void blankTriggerThrows() {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        config.set("abilities", java.util.List.of(
                java.util.Map.of("id", "blank_trigger", "unlock_level", 1, "trigger", "  ")
        ));
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config));
    }

    @Test
    void validTriggerParses() {
        String yaml = """
                id: "test"
                max_level: 10
                progression:
                  curve: "constant"
                  base_xp: 100
                abilities:
                  - id: "has_trigger"
                    display_name: "Has Trigger"
                    unlock_level: 1
                    trigger: "block_break"
                    mechanics:
                      - type: "core:yield_multiplier"
                        parameters:
                          yield_chance:
                            constant: 1.0
                    feedback:
                      notify:
                        action_bar: false
                        chat: false
                """;
        var config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        SkillDefinition def = skillManager.parseSkill(config);
        assertEquals(1, def.abilities().size());
        assertEquals("block_break", def.abilities().get(0).trigger());
        assertEquals("has_trigger", def.abilities().get(0).id());
    }

    @Test
    void unknownXpSourceTriggerFailsToLoad() {
        String yaml = """
                id: "test"
                max_level: 10
                progression:
                  curve: "constant"
                  base_xp: 100
                xp_sources:
                  - trigger: "block_braek"
                    reward:
                      constant: 10
                """;
        var config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("block_braek"), ex.getMessage());
    }

    @Test
    void validXpSourceTriggerParses() {
        String yaml = """
                id: "test"
                max_level: 10
                progression:
                  curve: "constant"
                  base_xp: 100
                xp_sources:
                  - trigger: "block_break"
                    reward:
                      constant: 10
                """;
        var config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        SkillDefinition def = skillManager.parseSkill(config);
        assertEquals(1, def.xpSources().size());
        assertEquals("block_break", def.xpSources().get(0).trigger());
    }

    @Test
    void oversizedMaxLevelFailsFast() {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("max_level", 100_000_000);
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("max_level"), ex.getMessage());
    }
}