package io.github.chasehuegel.skilling.web;

import io.github.chasehuegel.skilling.web.dto.FillerDTO;
import io.github.chasehuegel.skilling.web.dto.GuiLayoutDTO;
import io.github.chasehuegel.skilling.web.dto.GuiLayoutSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuiLayoutSerializerTest {

    @Test
    void nullContentReturnsEmpty() {
        GuiLayoutDTO dto = GuiLayoutSerializer.parse(null);
        assertEquals("&8\u2692 &6Skills &8\u2692", dto.title());
        assertEquals(6, dto.rows());
        assertEquals(1, dto.pages().size());
        assertEquals("&6Skills", dto.pages().getFirst().label());
    }

    @Test
    void blankContentReturnsEmpty() {
        GuiLayoutDTO dto = GuiLayoutSerializer.parse("   ");
        assertEquals(6, dto.rows());
        assertEquals(1, dto.pages().size());
    }

    @Test
    void parseNewFormat() {
        String yaml = """
            title: "&8⚒ &6Skills &8⚒"
            rows: 6
            version: 1
            pages:
              - label: "&eCombat"
                slots:
                  0: "swords"
                  1: "archery"
                  9: "axes"
              - label: "&aGathering"
                slots:
                  0: "mining"
                  1: "woodcutting"
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals("&8⚒ &6Skills &8⚒", dto.title());
        assertEquals(6, dto.rows());
        assertEquals(1, dto.version());
        assertEquals(2, dto.pages().size());

        GuiLayoutDTO.GuiPageDTO combat = dto.pages().get(0);
        assertEquals("&eCombat", combat.label());
        assertEquals(3, combat.slots().size());
        assertEquals("swords", combat.slots().get(0));
        assertEquals("archery", combat.slots().get(1));
        assertEquals("axes", combat.slots().get(9));

        GuiLayoutDTO.GuiPageDTO gathering = dto.pages().get(1);
        assertEquals("&aGathering", gathering.label());
        assertEquals(2, gathering.slots().size());
        assertEquals("mining", gathering.slots().get(0));
        assertEquals("woodcutting", gathering.slots().get(1));
    }

    @Test
    void parseLegacyFormat() {
        String yaml = """
            filler:
              material: "minecraft:black_stained_glass_pane"
            pages:
              gathering:
                title: "&6Gathering"
                icon: "minecraft:iron_pickaxe"
                rows: 0
                skills:
                  mining: 0
                  woodcutting: 1
                  excavation: 2
              combat:
                title: "&cCombat"
                icon: "minecraft:iron_sword"
                rows: 0
                skills:
                  heavy_weapons: 0
                  light_weapons: 1
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals(2, dto.pages().size());

        GuiLayoutDTO.GuiPageDTO gathering = dto.pages().get(0);
        assertEquals("&6Gathering", gathering.label());
        assertEquals(3, gathering.slots().size());
        assertEquals("mining", gathering.slots().get(0));
        assertEquals("woodcutting", gathering.slots().get(1));
        assertEquals("excavation", gathering.slots().get(2));

        GuiLayoutDTO.GuiPageDTO combat = dto.pages().get(1);
        assertEquals("&cCombat", combat.label());
        assertEquals(2, combat.slots().size());
        assertEquals("heavy_weapons", combat.slots().get(0));
        assertEquals("light_weapons", combat.slots().get(1));
    }

    @Test
    void parseLegacyFormatPreservesPageOrder() {
        String yaml = """
            pages:
              z_last:
                title: "&cZ"
                skills:
                  skill_z: 0
              a_first:
                title: "&aA"
                skills:
                  skill_a: 0
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals(2, dto.pages().size());
        assertEquals("&cZ", dto.pages().get(0).label());
        assertEquals("&aA", dto.pages().get(1).label());
    }

    @Test
    void roundTripPreservesData() {
        GuiLayoutDTO original = new GuiLayoutDTO(
            "&6Skills",
            6,
            java.util.List.of(
                new GuiLayoutDTO.GuiPageDTO("&eCombat", Map.of(0, "swords", 5, "archery"), "minecraft:book", 0),
                new GuiLayoutDTO.GuiPageDTO("&aGathering", Map.of(0, "mining"), "minecraft:book", 0)
            ),
            1
        );

        String yaml = GuiLayoutSerializer.serialize(original);

        // Serialized output is in legacy format — verify it contains expected structure
        assertTrue(yaml.contains("filler:"), "Should contain filler section");
        assertTrue(yaml.contains("pages:"), "Should contain pages section");
        assertTrue(yaml.contains("skills:"), "Should contain skills section");
        assertTrue(yaml.contains("swords"), "Should contain skill id 'swords'");

        // Parse back and verify data round-trips
        GuiLayoutDTO parsed = GuiLayoutSerializer.parse(yaml);
        assertEquals(original.pages().size(), parsed.pages().size());
        assertEquals(original.pages().get(0).label(), parsed.pages().get(0).label());
        assertEquals(original.pages().get(0).slots(), parsed.pages().get(0).slots());
        assertEquals(original.pages().get(1).label(), parsed.pages().get(1).label());
        assertEquals(original.pages().get(1).slots(), parsed.pages().get(1).slots());
    }

    @Test
    void roundTripLegacyThroughSerialize() {
        String legacyYaml = """
            pages:
              gathering:
                title: "&6Gathering"
                skills:
                  mining: 0
                  woodcutting: 1
            """;

        // Parse legacy to DTO
        GuiLayoutDTO dto = GuiLayoutSerializer.parse(legacyYaml);
        // Serialize (produces legacy format)
        String serialized = GuiLayoutSerializer.serialize(dto);
        // Re-parse
        GuiLayoutDTO reparsed = GuiLayoutSerializer.parse(serialized);

        assertEquals(dto.pages().size(), reparsed.pages().size());
        assertEquals(dto.pages().get(0).label(), reparsed.pages().get(0).label());
        assertEquals(dto.pages().get(0).slots(), reparsed.pages().get(0).slots());
    }

    @Test
    void emptyLayoutRoundTrip() {
        GuiLayoutDTO empty = GuiLayoutDTO.empty();
        String yaml = GuiLayoutSerializer.serialize(empty);
        GuiLayoutDTO parsed = GuiLayoutSerializer.parse(yaml);

        assertEquals(1, parsed.pages().size());
        assertTrue(parsed.pages().getFirst().slots().isEmpty());
    }

    @Test
    void parseLegacyFormatPreservesIconAndCmd() {
        String yaml = """
            pages:
              gathering:
                title: "&6Gathering"
                icon: "minecraft:iron_pickaxe"
                custom_model_data: 1001
                skills:
                  mining: 0
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals(1, dto.pages().size());
        assertEquals("minecraft:iron_pickaxe", dto.pages().get(0).icon());
        assertEquals(1001, dto.pages().get(0).customModelData());
    }

    @Test
    void serializedYamlContainsExpectedKeys() {
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 3, java.util.List.of(
            new GuiLayoutDTO.GuiPageDTO("Page1", Map.of(0, "skill_a"), "minecraft:book", 0)
        ), 1);

        String yaml = GuiLayoutSerializer.serialize(dto);
        assertTrue(yaml.contains("filler:"), "Missing filler:");
        assertTrue(yaml.contains("pages:"), "Missing pages:");
        assertTrue(yaml.contains("title:"), "Missing title:");
        assertTrue(yaml.contains("skills:"), "Missing skills:");
        // Should contain the inverted skill mapping: skill_a: 0 (skill_id → slot_index)
        assertTrue(yaml.contains("skill_a: 0"), "Missing inverted skill mapping");
    }

    @Test
    void handlesMissingFieldsGracefully() {
        String yaml = """
            pages:
              - label: "Test"
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        // Falls back to sensible defaults
        assertNotNull(dto.title());
        assertEquals(6, dto.rows());
        assertEquals(1, dto.version());
        assertEquals(1, dto.pages().size());
        assertEquals("Test", dto.pages().get(0).label());
    }

    @Test
    void serializeInvertsSlotMapping() {
        // DTO stores slot_index → skill_id
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 6, java.util.List.of(
            new GuiLayoutDTO.GuiPageDTO("Page1", Map.of(
                0, "mining",
                5, "woodcutting",
                22, "farming"
            ), "minecraft:book", 0)
        ), 1);

        String yaml = GuiLayoutSerializer.serialize(dto);

        // The legacy format should map skill_id → slot_index
        // So we should see "mining: 0", "woodcutting: 5", "farming: 22"
        assertTrue(yaml.contains("mining: 0"));
        assertTrue(yaml.contains("woodcutting: 5"));
        assertTrue(yaml.contains("farming: 22"));
    }

    @Test
    void serializeWritesRowsAndVersion() {
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 4, java.util.List.of(
            new GuiLayoutDTO.GuiPageDTO("Page1", Map.of(0, "skill_a"), "minecraft:book", 0)
        ), 2);

        String yaml = GuiLayoutSerializer.serialize(dto);

        assertTrue(yaml.contains("rows: 4"), "Missing top-level rows");
        assertTrue(yaml.contains("version: 2"), "Missing version");
        assertTrue(yaml.contains("rows: 4"), "Missing per-page rows");
    }

    @Test
    void roundTripIsLosslessForRowsVersionAndFiller() {
        FillerDTO filler = new FillerDTO("minecraft:gray_stained_glass_pane", 42);
        GuiLayoutDTO original = new GuiLayoutDTO(
            "&6Skills",
            4,
            java.util.List.of(
                new GuiLayoutDTO.GuiPageDTO("&eCombat", Map.of(0, "swords", 5, "archery"), "minecraft:book", 0)
            ),
            3,
            filler
        );

        String once = GuiLayoutSerializer.serialize(original);
        GuiLayoutDTO parsed = GuiLayoutSerializer.parse(once);
        String twice = GuiLayoutSerializer.serialize(parsed);

        assertEquals(4, parsed.rows(), "rows must survive serialize -> parse");
        assertEquals(3, parsed.version(), "version must survive serialize -> parse");
        assertEquals("minecraft:gray_stained_glass_pane", parsed.filler().material(), "filler material must survive");
        assertEquals(42, parsed.filler().customModelData(), "filler custom_model_data must survive");

        assertEquals(once, twice, "serialize -> parse -> serialize must be idempotent");
    }

    @Test
    void legacyFillerIsPreservedThroughRoundTrip() {
        String yaml = """
            filler:
              material: "minecraft:red_stained_glass_pane"
              custom_model_data: 7
            pages:
              gathering:
                title: "&6Gathering"
                skills:
                  mining: 0
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals("minecraft:red_stained_glass_pane", dto.filler().material());
        assertEquals(7, dto.filler().customModelData());

        GuiLayoutDTO reparsed = GuiLayoutSerializer.parse(GuiLayoutSerializer.serialize(dto));
        assertEquals("minecraft:red_stained_glass_pane", reparsed.filler().material());
        assertEquals(7, reparsed.filler().customModelData());
    }

    @Test
    void guiTitleSurvivesLegacyRoundTrip() {
        String yaml = """
            pages:
              gathering:
                title: "&6Gathering"
                gui_title: "&eGathering Skills"
                skills:
                  mining: 0
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals("&eGathering Skills", dto.pages().get(0).guiTitle());

        GuiLayoutDTO reparsed = GuiLayoutSerializer.parse(GuiLayoutSerializer.serialize(dto));
        assertEquals("&eGathering Skills", reparsed.pages().get(0).guiTitle(),
                "per-page gui_title must survive a web round-trip");
    }

    @Test
    void guiTitleSurvivesNewFormatRoundTrip() {
        GuiLayoutDTO original = new GuiLayoutDTO("Test", 6, java.util.List.of(
            new GuiLayoutDTO.GuiPageDTO("&eCombat", Map.of(0, "swords"), "minecraft:book", 0, "&cCombat Skills")
        ), 1);

        GuiLayoutDTO reparsed = GuiLayoutSerializer.parse(GuiLayoutSerializer.serialize(original));
        assertEquals("&cCombat Skills", reparsed.pages().get(0).guiTitle());
    }

    @Test
    void absentGuiTitleStaysNull() {
        GuiLayoutDTO dto = GuiLayoutSerializer.parse("""
            pages:
              gathering:
                title: "&6Gathering"
                skills:
                  mining: 0
            """);
        assertNull(dto.pages().get(0).guiTitle());
    }
}
