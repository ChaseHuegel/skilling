package io.github.chasehuegel.skilling.web.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.EvaluatorDTO;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.ExhaustionDTO;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.ItemRequirementDTO;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.RequirementsDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Jackson deserializer for {@link RequirementsDTO} that accepts the {@code cooldown}
 * field in either the evaluator-object shape ({@code {type, params}}) returned by
 * {@code GET} or a plain JSON number (what the skill editor submits). Numbers are
 * wrapped in a constant evaluator, mirroring {@link SkillSerializer#parseCooldown}.
 *
 * <p>Registered on {@link RequirementsDTO} via {@code @JsonDeserialize}; keeps the
 * DTO strongly typed rather than widening {@code cooldown} to {@code Object}.
 */
final class RequirementsDTODeserializer extends JsonDeserializer<RequirementsDTO> {

    @Override
    public RequirementsDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        JsonNode cooldownNode = node.get("cooldown");
        EvaluatorDTO cooldown = parseCooldown(p, cooldownNode);

        List<String> state = parseStringList(node.get("state"));
        List<ItemRequirementDTO> items = parseItems(node.get("items"));
        ExhaustionDTO exhaustion = parseExhaustion(node.get("exhaustion"));

        return new RequirementsDTO(cooldown, state, items, exhaustion);
    }

    private static EvaluatorDTO parseCooldown(JsonParser p, JsonNode node) throws IOException {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return new EvaluatorDTO("constant", Map.of("value", 0.0));
        }
        if (node.isNumber()) {
            return new EvaluatorDTO("constant", Map.of("value", node.doubleValue()));
        }
        if (node.isObject()) {
            JsonNode typeNode = node.get("type");
            JsonNode paramsNode = node.get("params");
            String type = typeNode != null ? typeNode.asText() : "constant";
            Map<String, Object> params = paramsNode != null && paramsNode.isObject()
                    ? p.getCodec().treeToValue(paramsNode, Map.class)
                    : Map.of();
            return new EvaluatorDTO(type, params);
        }
        return new EvaluatorDTO("constant", Map.of("value", 0.0));
    }

    private static List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) result.add(item.asText());
        });
        return result;
    }

    private static List<ItemRequirementDTO> parseItems(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<ItemRequirementDTO> result = new ArrayList<>();
        node.forEach(item -> {
            if (!item.isObject()) return;
            result.add(new ItemRequirementDTO(
                    textValue(item, "action", "possession"),
                    textValue(item, "tag", null),
                    textValue(item, "slot", "HAND"),
                    (int) doubleValue(item, "amount", 1),
                    doubleValue(item, "item_cooldown", 0)
            ));
        });
        return result;
    }

    private static ExhaustionDTO parseExhaustion(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        return new ExhaustionDTO(
                doubleValue(node, "amount", 1.0),
                doubleValue(node, "minimum", 0.0)
        );
    }

    private static String textValue(JsonNode node, String key, String def) {
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) return def;
        return v.asText();
    }

    private static double doubleValue(JsonNode node, String key, double def) {
        JsonNode v = node.get(key);
        if (v == null || !v.isNumber()) return def;
        return v.doubleValue();
    }
}
