package io.github.chasehuegel.skilling.web.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies {@link SkillSerializer} round-trips string-valued evaluator
 * constants (e.g. {@code effect: { constant: "minecraft:slowness" }}) through
 * the web DTO without throwing or coercing them to numeric zero.
 */
class SkillSerializerEvaluatorConstantTest {

    @Test
    void parseEvaluatorPreservesStringConstant() {
        var dto = SkillSerializer.parseEvaluator(Map.of("constant", "minecraft:slowness"));
        assertEquals("constant", dto.type());
        assertEquals("minecraft:slowness", dto.params().get("value"));
        assertInstanceOf(String.class, dto.params().get("value"));
    }

    @Test
    void parseEvaluatorPreservesNestedStringConstant() {
        var dto = SkillSerializer.parseEvaluator(Map.of("constant", Map.of("value", "minecraft:shield")));
        assertEquals("minecraft:shield", dto.params().get("value"));
    }

    @Test
    void evaluatorToMapRoundTripsStringConstant() {
        var dto = new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", "minecraft:slowness"));
        Map<String, Object> map = SkillSerializer.evaluatorToMap(dto);
        assertEquals("minecraft:slowness", map.get("constant"));
        assertInstanceOf(String.class, map.get("constant"));
    }

    @Test
    void parseAndSerializeRoundTripStringConstant() {
        var dto = SkillSerializer.parseEvaluator(Map.of("constant", "minecraft:poison"));
        Map<String, Object> map = SkillSerializer.evaluatorToMap(dto);
        assertEquals(Map.of("constant", "minecraft:poison"), map);
    }

    @Test
    void numericConstantStillParsesAsNumber() {
        var dto = SkillSerializer.parseEvaluator(Map.of("constant", 3));
        assertEquals(3.0, dto.params().get("value"));
        assertInstanceOf(Double.class, dto.params().get("value"));
    }

    @Test
    void numericConstantStillSerializesAsNumber() {
        var dto = new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", 3.0));
        Map<String, Object> map = SkillSerializer.evaluatorToMap(dto);
        assertEquals(Map.of("value", 3.0), map.get("constant"));
    }
}
