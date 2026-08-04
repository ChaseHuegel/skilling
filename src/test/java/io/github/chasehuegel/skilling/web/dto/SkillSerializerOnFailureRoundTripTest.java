package io.github.chasehuegel.skilling.web.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies on_failure feedback round-trips through the web DTO, including the
 * {@code exhaustion} reason the editor dropdown now exposes.
 */
class SkillSerializerOnFailureRoundTripTest {

    @Test
    void exhaustionOnFailureSurvivesRoundTrip() {
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
                    mechanics: []
                    on_failure:
                      exhaustion:
                        action_bar: "&cToo exhausted!"
                        sounds:
                          - { type: "minecraft:entity.player.levelup", volume: 0.5, pitch: 1.0, target: "self" }
                    feedback: { notify: { action_bar: false } }
                """;

        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        var reasons = dto.abilities().get(0).onFailure().reasons();
        assertTrue(reasons.containsKey("exhaustion"), "exhaustion on_failure must be parsed");

        SkillDetailDTO reparsed = SkillSerializer.fromYaml(SkillSerializer.toYaml(dto));
        var reparsedReasons = reparsed.abilities().get(0).onFailure().reasons();
        assertEquals("&cToo exhausted!", reparsedReasons.get("exhaustion").actionBar());
        assertEquals(1, reparsedReasons.get("exhaustion").sounds().size());
    }

    @Test
    void cooldownOnFailureStillRoundTrips() {
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
                    mechanics: []
                    on_failure:
                      cooldown:
                        action_bar: "&cCooling down: {time}s"
                    feedback: { notify: { action_bar: false } }
                """;

        SkillDetailDTO reparsed = SkillSerializer.fromYaml(SkillSerializer.toYaml(SkillSerializer.fromYaml(yaml)));
        assertEquals("&cCooling down: {time}s",
                reparsed.abilities().get(0).onFailure().reasons().get("cooldown").actionBar());
    }
}
