package io.github.chasehuegel.skilling.web.dto;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the {@code scaling} key survives the web serializer round-trip: a
 * {@code scaling: damage} source re-parses to {@code XpScaling.DAMAGE} after a
 * web save, and a flat source stays {@code NONE} with the key omitted from the
 * emitted YAML.
 */
class SkillSerializerXpScalingRoundTripTest {

    private SkillDefinition engineParse(String yaml) {
        SkillManager sm = TestSkillManager.newBuiltIn();
        return sm.parseSkill(YamlConfiguration.loadConfiguration(new StringReader(yaml)));
    }

    private String roundTripYaml(String yaml) {
        return SkillSerializer.toYaml(SkillSerializer.fromYaml(yaml));
    }

    @Test
    void damageScalingSurvivesWebRoundTrip() {
        String yaml = """
                id: test
                max_level: 10
                progression: { curve: constant, base_xp: 100 }
                xp_sources:
                  - trigger: fall_damage
                    reward: { constant: 14.0 }
                    scaling: damage
                """;
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        assertEquals("damage", dto.xpSources().get(0).scaling());

        SkillDefinition def = engineParse(roundTripYaml(yaml));
        assertEquals(SkillDefinition.XpScaling.DAMAGE, def.xpSources().get(0).scaling());
    }

    @Test
    void flatSourceStaysNoneAndOmitsScalingKey() {
        String yaml = """
                id: test
                max_level: 10
                progression: { curve: constant, base_xp: 100 }
                xp_sources:
                  - trigger: block_break
                    reward: { constant: 50.0 }
                """;
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        assertEquals("none", dto.xpSources().get(0).scaling());

        String roundTripped = roundTripYaml(yaml);
        assertFalse(roundTripped.contains("scaling"),
                "a flat source must not emit a scaling key after a web save");
        SkillDefinition def = engineParse(roundTripped);
        assertEquals(SkillDefinition.XpScaling.NONE, def.xpSources().get(0).scaling());
    }
}
