package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies ability lore placeholders fail fast at config-load when they do not
 * resolve against the ability's mechanic parameter keys (rather than warning
 * lazily at GUI-open time and rendering raw tokens).
 */
class SkillManagerLorePlaceholderTest {

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
    }

    private YamlConfiguration skillWithAbility(List<Map<String, Object>> mechanics, List<String> lore) {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        config.set("abilities", List.of(Map.of(
                "id", "a1",
                "display_name", "Ability",
                "trigger", "block_break",
                "display", Map.of("lore", lore),
                "mechanics", mechanics
        )));
        return config;
    }

    @Test
    void knownPlaceholderParses() {
        var config = skillWithAbility(List.of(Map.<String, Object>of(
                "type", "core:xp_bonus",
                "parameters", Map.of("multiplier", Map.of("constant", 1.5))
        )), List.of("Gain &a{multiplier}x&7 XP."));
        SkillDefinition def = skillManager.parseSkill(config);
        assertEquals(1, def.abilities().size());
    }

    @Test
    void unknownPlaceholderFailsLoad() {
        var config = skillWithAbility(List.of(Map.<String, Object>of(
                "type", "core:xp_bonus",
                "parameters", Map.of("multiplier", Map.of("constant", 1.5))
        )), List.of("Gain &a{not_a_param}x&7 XP."));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("{not_a_param}"), ex.getMessage());
        assertTrue(ex.getMessage().contains("multiplier"), ex.getMessage());
    }

    @Test
    void noMechanicParamsMeansAnyPlaceholderFails() {
        var config = skillWithAbility(List.of(Map.<String, Object>of("type", "core:auto_replant")),
                List.of("Chance: {replant_chance}%"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(config));
        assertTrue(ex.getMessage().contains("{replant_chance}"), ex.getMessage());
    }

    @Test
    void emptyLoreSkipsValidation() {
        var config = skillWithAbility(List.of(Map.<String, Object>of("type", "core:auto_replant")), List.of());
        SkillDefinition def = skillManager.parseSkill(config);
        assertEquals(1, def.abilities().size());
    }
}
