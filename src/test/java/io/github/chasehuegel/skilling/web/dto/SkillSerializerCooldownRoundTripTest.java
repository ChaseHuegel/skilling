package io.github.chasehuegel.skilling.web.dto;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies cooldown requirement evaluators (linear/milestone blocks) survive
 * the web DTO round-trip: the web serializer must preserve them as evaluator
 * objects so a reload keeps the dynamic cooldown instead of resetting it to 0.
 */
class SkillSerializerCooldownRoundTripTest {

    private SkillDefinition engineParse(String yaml) {
        SkillManager sm = TestSkillManager.newBuiltIn();
        return sm.parseSkill(YamlConfiguration.loadConfiguration(new StringReader(yaml)));
    }

    private SkillDefinition roundTrip(String yaml) {
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        return engineParse(SkillSerializer.toYaml(dto));
    }

    private static String skillWithCooldown(String cooldownYaml) {
        return "id: test\n"
            + "max_level: 100\n"
            + "display: { name: \"Test\", color: \"GREEN\", style: \"SOLID\" }\n"
            + "progression: { curve: \"polynomial\", base_xp: 50, exponent: 2.5 }\n"
            + "abilities:\n"
            + "  - id: a\n"
            + "    display_name: \"A\"\n"
            + "    unlock_level: 1\n"
            + "    trigger: \"block_break\"\n"
            + "    requirements:\n"
            + "      cooldown:\n"
            + cooldownYaml
            + "    mechanics: []\n"
            + "    feedback: { notify: { action_bar: false } }\n";
    }

    @Test
    void linearCooldownRoundTripsThroughWebAndEngine() {
        String yaml = skillWithCooldown("        linear: { base: 5.0, step: -0.02, max: 1.0 }\n");
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        assertEquals("linear", dto.abilities().get(0).requirements().cooldown().type());

        // The web round-trip must yield an evaluator behaving identically to a
        // direct engine parse of the same YAML (clamped at max 1).
        var direct = engineParse(yaml).abilities().get(0).requirements().cooldown();
        var round = roundTrip(yaml).abilities().get(0).requirements().cooldown();
        for (int level : new int[]{1, 5, 50, 300}) {
            assertEquals(direct.evaluate(level, 1), round.evaluate(level, 1), 1e-9);
        }
    }

    @Test
    void milestoneCooldownRoundTripsThroughWebAndEngine() {
        String yaml = skillWithCooldown("        milestones: { 15: 3.0, 40: 8.0 }\n");
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        assertEquals("milestones", dto.abilities().get(0).requirements().cooldown().type());

        SkillDefinition def = roundTrip(yaml);
        var cd = def.abilities().get(0).requirements().cooldown();
        assertEquals(3.0, cd.evaluate(15, 1), 1e-9);
        assertEquals(8.0, cd.evaluate(40, 1), 1e-9);
    }

    @Test
    void constantCooldownRoundTripsAsScalar() {
        String yaml = skillWithCooldown("        constant: 7.0\n");
        SkillDefinition def = roundTrip(yaml);
        var cd = def.abilities().get(0).requirements().cooldown();
        assertEquals(7.0, cd.evaluate(10, 1), 1e-9);
    }
}
