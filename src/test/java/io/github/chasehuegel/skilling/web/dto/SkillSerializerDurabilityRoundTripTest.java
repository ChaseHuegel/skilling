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
 * Verifies the {@code durability} requirement (flat, level-scaled point cost) and
 * the {@code enchanted: true} item predicate survive the web DTO round-trip, so a
 * reload does not drop them from a skill's requirement block.
 */
class SkillSerializerDurabilityRoundTripTest {

    private SkillDefinition engineParse(String yaml) {
        SkillManager sm = TestSkillManager.newBuiltIn();
        return sm.parseSkill(YamlConfiguration.loadConfiguration(new StringReader(yaml)));
    }

    private SkillDefinition roundTrip(String yaml) {
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        return engineParse(SkillSerializer.toYaml(dto));
    }

    private static String skillWith(String requirementsYaml) {
        return "id: test\n"
            + "max_level: 100\n"
            + "display: { name: \"Test\", color: \"GREEN\", style: \"SOLID\" }\n"
            + "progression: { curve: \"polynomial\", base_xp: 50, exponent: 2.5 }\n"
            + "abilities:\n"
            + "  - id: a\n"
            + "    display_name: \"A\"\n"
            + "    unlock_level: 25\n"
            + "    trigger: \"right_click_block\"\n"
            + "    requirements:\n"
            + requirementsYaml
            + "    mechanics:\n"
            + "      - type: \"core:extract_enchant\"\n"
            + "    feedback: { notify: { action_bar: false } }\n";
    }

    @Test
    void linearDurabilityRoundTripsThroughWebAndEngine() {
        String yaml = skillWith(
            "      durability:\n"
            + "        amount: { linear: { base: 30.0, step: -0.2, max: 10.0 } }\n"
            + "        slot: \"MAIN_HAND\"\n");
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        assertEquals("linear", dto.abilities().get(0).requirements().durability().amount().type());

        var direct = engineParse(yaml).abilities().get(0).requirements().durability().amount();
        var round = roundTrip(yaml).abilities().get(0).requirements().durability().amount();
        for (int level : new int[]{25, 50, 75, 100}) {
            assertEquals(direct.evaluate(level, 25), round.evaluate(level, 25), 1e-9);
        }
    }

    @Test
    void enchantedItemPredicateSurvivesRoundTrip() {
        String yaml = skillWith(
            "      items:\n"
            + "        - { action: \"possession\", tag: \"#minecraft:swords\", slot: \"MAIN_HAND\", enchanted: true }\n");
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        assertTrue(dto.abilities().get(0).requirements().items().get(0).enchanted());

        var items = roundTrip(yaml).abilities().get(0).requirements().items();
        assertTrue(items.get(0).enchanted());
    }
}
