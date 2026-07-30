package io.github.chasehuegel.skilling.web;

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
    void parseFullLayout() {
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
    void roundTripPreservesData() {
        GuiLayoutDTO original = new GuiLayoutDTO(
            "&6Skills",
            6,
            java.util.List.of(
                new GuiLayoutDTO.GuiPageDTO("&eCombat", Map.of(0, "swords", 5, "archery")),
                new GuiLayoutDTO.GuiPageDTO("&aGathering", Map.of(0, "mining"))
            ),
            1
        );

        String yaml = GuiLayoutSerializer.serialize(original);
        GuiLayoutDTO parsed = GuiLayoutSerializer.parse(yaml);

        assertEquals(original.title(), parsed.title());
        assertEquals(original.rows(), parsed.rows());
        assertEquals(original.version(), parsed.version());
        assertEquals(original.pages().size(), parsed.pages().size());
        assertEquals(original.pages().get(0).label(), parsed.pages().get(0).label());
        assertEquals(original.pages().get(0).slots(), parsed.pages().get(0).slots());
        assertEquals(original.pages().get(1).label(), parsed.pages().get(1).label());
        assertEquals(original.pages().get(1).slots(), parsed.pages().get(1).slots());
    }

    @Test
    void emptyLayoutRoundTrip() {
        GuiLayoutDTO empty = GuiLayoutDTO.empty();
        String yaml = GuiLayoutSerializer.serialize(empty);
        GuiLayoutDTO parsed = GuiLayoutSerializer.parse(yaml);

        assertEquals(empty.title(), parsed.title());
        assertEquals(empty.rows(), parsed.rows());
        assertEquals(1, parsed.pages().size());
        assertTrue(parsed.pages().getFirst().slots().isEmpty());
    }

    @Test
    void serializedYamlContainsExpectedKeys() {
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 3, java.util.List.of(
            new GuiLayoutDTO.GuiPageDTO("Page1", Map.of(0, "skill_a"))
        ), 1);

        String yaml = GuiLayoutSerializer.serialize(dto);
        assertTrue(yaml.contains("title:"));
        assertTrue(yaml.contains("rows:"));
        assertTrue(yaml.contains("version:"));
        assertTrue(yaml.contains("pages:"));
        assertTrue(yaml.contains("label:"));
        assertTrue(yaml.contains("slots:"));
    }

    @Test
    void handlesMissingFieldsGracefully() {
        String yaml = """
            pages:
              - label: "Test"
            """;

        GuiLayoutDTO dto = GuiLayoutSerializer.parse(yaml);
        assertEquals("", dto.title());
        assertEquals(6, dto.rows());
        assertEquals(1, dto.version());
        assertEquals(1, dto.pages().size());
    }
}
