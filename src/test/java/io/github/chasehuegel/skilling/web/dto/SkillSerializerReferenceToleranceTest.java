package io.github.chasehuegel.skilling.web.dto;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the serializers tolerate id-referenced abilities: parsing an
 * ability with an id and no trigger must not error, and a no-op round-trip
 * must not add spurious overrides that would mask the registered ability.
 */
class SkillSerializerReferenceToleranceTest {

    @Test
    void bundledMiningReferenceParsesWithoutError() throws Exception {
        java.net.URL url = getClass().getClassLoader().getResource("skills/mining.yml");
        assertNotNull(url, "mining.yml must exist on the test classpath");
        File file = new File(url.toURI());

        SkillDetailDTO dto = SkillSerializer.parseSkillFile(file);
        SkillDetailDTO.AbilityDTO veinMiner = dto.abilities().stream()
                .filter(a -> a.id().equals("vein_miner"))
                .findFirst().orElseThrow();
        assertNull(veinMiner.trigger(), "the id reference carries no trigger");
        assertEquals(25, veinMiner.unlockLevel());
    }

    @Test
    void referenceRoundTripsWithoutSpuriousOverrides() {
        String yaml = """
                id: mining
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                abilities:
                  - id: vein_miner
                    unlock_level: 25
                """;
        SkillDetailDTO dto = SkillSerializer.fromYaml(yaml);
        String out = SkillSerializer.toYaml(dto);
        String abilityBlock = out.substring(out.indexOf("- id: vein_miner"));

        assertTrue(abilityBlock.contains("id: vein_miner"), out);
        assertTrue(abilityBlock.contains("unlock_level: 25"), out);
        assertFalse(abilityBlock.contains("trigger"),
                "a reference must not gain a spurious trigger: " + out);
        assertFalse(abilityBlock.contains("display_name"),
                "a reference must not gain a spurious display_name: " + out);
        assertFalse(abilityBlock.contains("feedback"),
                "a reference must not gain a spurious feedback block: " + out);

        SkillDetailDTO reparsed = SkillSerializer.fromYaml(out);
        assertEquals(25, reparsed.abilities().get(0).unlockLevel());
        assertNull(reparsed.abilities().get(0).trigger());
    }

    @Test
    void fullInlineAbilityStillRoundTripsAllFields() {
        String yaml = """
                id: test
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                abilities:
                  - id: abil
                    display_name: "Abil"
                    unlock_level: 5
                    trigger: "block_break"
                    mechanics:
                      - type: "core:block_particles"
                        parameters:
                          particle: { constant: "minecraft:happy_villager" }
                    feedback: { notify: { action_bar: true, chat: false, message: "hi" } }
                """;
        SkillDetailDTO reparsed = SkillSerializer.fromYaml(SkillSerializer.toYaml(SkillSerializer.fromYaml(yaml)));
        var ab = reparsed.abilities().get(0);
        assertEquals("block_break", ab.trigger());
        assertEquals("Abil", ab.displayName());
        assertEquals(5, ab.unlockLevel());
        assertEquals(1, ab.mechanics().size());
        assertTrue(ab.feedback().actionBar());
    }
}
