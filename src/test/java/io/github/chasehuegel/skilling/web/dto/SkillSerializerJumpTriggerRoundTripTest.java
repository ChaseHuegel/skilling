package io.github.chasehuegel.skilling.web.dto;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@code success_only} feedback flag and the {@code jump} trigger
 * survive the web DTO round-trip, so an admin saving the ability through the GUI
 * keeps both instead of resetting them.
 */
class SkillSerializerJumpTriggerRoundTripTest {

    private SkillDefinition engineParse(String yaml) {
        SkillManager sm = TestSkillManager.newBuiltIn();
        return sm.parseSkill(YamlConfiguration.loadConfiguration(new StringReader(yaml)));
    }

    private SkillDefinition roundTrip(String yaml) {
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        return engineParse(SkillSerializer.toYaml(dto));
    }

    @Test
    void successOnlyFeedbackSurvivesRoundTrip() {
        String yaml = "id: test\n"
            + "max_level: 100\n"
            + "display: { name: \"Test\", color: \"GREEN\", style: \"SOLID\" }\n"
            + "progression: { curve: \"polynomial\", base_xp: 50, exponent: 2.5 }\n"
            + "abilities:\n"
            + "  - id: dodge\n"
            + "    display_name: \"Dodge\"\n"
            + "    unlock_level: 1\n"
            + "    trigger: \"entity_damage_taken\"\n"
            + "    mechanics:\n"
            + "      - type: \"core:dodge\"\n"
            + "        parameters:\n"
            + "          chance: { constant: 10.0 }\n"
            + "    feedback:\n"
            + "      notify: { action_bar: true, chat: false, message: \"&fDodged!\" }\n"
            + "      success_only: true\n";

        SkillDefinition def = roundTrip(yaml);
        SkillDefinition.Feedback fb = def.abilities().get(0).feedback();
        assertTrue(fb.successOnly(), "success_only must survive the web round-trip");
        assertTrue(fb.actionBar());
    }

    @Test
    void jumpTriggerSurvivesRoundTrip() {
        String yaml = "id: test\n"
            + "max_level: 100\n"
            + "display: { name: \"Test\", color: \"GREEN\", style: \"SOLID\" }\n"
            + "progression: { curve: \"polynomial\", base_xp: 50, exponent: 2.5 }\n"
            + "abilities:\n"
            + "  - id: leap\n"
            + "    display_name: \"Leap\"\n"
            + "    unlock_level: 1\n"
            + "    trigger: \"jump\"\n"
            + "    mechanics:\n"
            + "      - type: \"core:speed_bonus\"\n"
            + "        parameters:\n"
            + "          multiplier: { constant: 1.2 }\n"
            + "    feedback: { notify: { action_bar: false } }\n";

        SkillDefinition def = roundTrip(yaml);
        assertEquals("jump", def.abilities().get(0).trigger());
    }
}
