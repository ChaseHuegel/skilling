package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.SkillDefinition.XpScaling;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@code scaling} key on XP sources: {@code scaling: damage} parses
 * only on triggers whose event carries damage ({@code fall_damage},
 * {@code entity_damage_taken}, {@code entity_damage}) and is rejected fail-fast
 * elsewhere or for unknown values.
 */
class SkillManagerXpScalingTest {

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
    }

    private XpScaling scalingFor(String trigger, String scalingYaml) {
        String yaml = """
                id: "test"
                max_level: 10
                progression:
                  curve: "constant"
                  base_xp: 100
                xp_sources:
                  - trigger: "%s"
                    reward:
                      constant: 10
                    %s
                """.formatted(trigger, scalingYaml);
        var config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        SkillDefinition def = skillManager.parseSkill(config);
        return def.xpSources().get(0).scaling();
    }

    @Test
    void damageScalingParsesOnFallDamage() {
        assertEquals(XpScaling.DAMAGE, scalingFor("fall_damage", "scaling: damage"));
    }

    @Test
    void damageScalingParsesOnEntityDamageTaken() {
        assertEquals(XpScaling.DAMAGE, scalingFor("entity_damage_taken", "scaling: damage"));
    }

    @Test
    void damageScalingParsesOnOutgoingEntityDamage() {
        // entity_damage fires on EntityDamageByEntityEvent, a damage event subclass.
        assertEquals(XpScaling.DAMAGE, scalingFor("entity_damage", "scaling: damage"));
    }

    @Test
    void absentScalingDefaultsToNone() {
        assertEquals(XpScaling.NONE, scalingFor("block_break", ""));
    }

    @Test
    void damageScalingRejectedOnNonDamageTrigger() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scalingFor("block_break", "scaling: damage"));
        assertTrue(ex.getMessage().contains("scaling"), ex.getMessage());
        assertTrue(ex.getMessage().contains("block_break"), ex.getMessage());
    }

    @Test
    void unknownScalingValueRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scalingFor("fall_damage", "scaling: nope"));
        assertTrue(ex.getMessage().contains("scaling"), ex.getMessage());
    }
}
