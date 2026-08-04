package io.github.chasehuegel.skilling.web.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies milestone evaluators round-trip between the engine's map shape
 * ({@code milestones: { 25: 3 }}) and the web editor's array-of-rows shape
 * ({@code [{level, value}]}). The web serializer must always normalize to the
 * map form the engine's {@code parseInlineEvaluator} expects, and must not
 * throw when a legacy list value is read back.
 */
class SkillSerializerMilestoneTest {

    @Test
    void parseEvaluatorNormalizesMapMilestones() {
        var dto = SkillSerializer.parseEvaluator(Map.of("milestones", Map.of(25, 3, 50, 8)));
        assertEquals("milestones", dto.type());
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) dto.params().get("milestones");
        assertEquals(3.0, ((Number) milestones.get("25")).doubleValue());
        assertEquals(8.0, ((Number) milestones.get("50")).doubleValue());
    }

    @Test
    void parseEvaluatorNormalizesListMilestonesWithoutThrowing() {
        // A list-valued milestones block (a web-editor array payload) must not
        // cause the ClassCastException that previously made GET return 500.
        var dto = SkillSerializer.parseEvaluator(Map.of("milestones",
                List.of(Map.of("level", 25, "value", 3), Map.of("level", 50, "value", 8))));
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) dto.params().get("milestones");
        assertEquals(3.0, ((Number) milestones.get("25")).doubleValue());
        assertEquals(8.0, ((Number) milestones.get("50")).doubleValue());
    }

    @Test
    void evaluatorToMapEmitsMapForMilestones() {
        var dto = new SkillDetailDTO.EvaluatorDTO("milestones",
                Map.of("milestones", Map.of("25", 3, "50", 8)));
        Map<String, Object> map = SkillSerializer.evaluatorToMap(dto);
        Map<String, Object> milestones = (Map<String, Object>) map.get("milestones");
        assertEquals(3, ((Number) milestones.get("25")).intValue());
        assertEquals(8, ((Number) milestones.get("50")).intValue());
    }

    @Test
    void evaluatorToMapNormalizesEditorArrayToMap() {
        // The editor sends array rows; serialization must normalize to a map so
        // the engine never receives a list.
        var dto = new SkillDetailDTO.EvaluatorDTO("milestones",
                Map.of("milestones", List.of(
                        Map.of("_key", "k1", "level", 25, "value", 3),
                        Map.of("_key", "k2", "level", 50, "value", 8))));
        Map<String, Object> map = SkillSerializer.evaluatorToMap(dto);
        Map<String, Object> milestones = (Map<String, Object>) map.get("milestones");
        assertInstanceOf(Map.class, map.get("milestones"), "milestones must serialize as a map");
        assertEquals(3, ((Number) milestones.get("25")).intValue());
        assertEquals(8, ((Number) milestones.get("50")).intValue());
    }

    @Test
    void fullSkillRoundTripPreservesMilestoneThresholds() {
        String yaml = """
                id: mining
                max_level: 100
                display: { name: "Mining", color: "GREEN", style: "SOLID" }
                abilities:
                  - id: vein_miner
                    display_name: "Vein Miner"
                    unlock_level: 15
                    trigger: "block_break"
                    mechanics:
                      - type: "core:chain_break"
                        parameters:
                          chain_limit:
                            milestones:
                              15: 3
                              40: 8
                              80: 16
                    feedback: { notify: { action_bar: false } }
                """;

        SkillDetailDTO parsed = SkillSerializer.fromYaml(yaml);
        var evaluator = parsed.abilities().get(0).mechanics().get(0).parameters().get("chain_limit");
        assertEquals("milestones", evaluator.type());

        // Serialize back and re-parse; the thresholds must be identical.
        SkillDetailDTO reparsed = SkillSerializer.fromYaml(SkillSerializer.toYaml(parsed));
        var reEvaluator = reparsed.abilities().get(0).mechanics().get(0).parameters().get("chain_limit");
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) reEvaluator.params().get("milestones");
        assertEquals(3, ((Number) milestones.get("15")).intValue());
        assertEquals(8, ((Number) milestones.get("40")).intValue());
        assertEquals(16, ((Number) milestones.get("80")).intValue());
    }

    @Test
    void emptyMilestonesSerializesAsEmptyMap() {
        var dto = new SkillDetailDTO.EvaluatorDTO("milestones", Map.of());
        Map<String, Object> map = SkillSerializer.evaluatorToMap(dto);
        assertEquals(Map.of(), map.get("milestones"));
    }

    @Test
    void nullMilestonesNormalizesToEmptyMap() {
        assertEquals(Map.of(), SkillSerializer.normalizeMilestones(null));
    }
}
