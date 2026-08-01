package io.github.chasehuegel.skilling.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.RequirementsDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies {@link RequirementsDTO} deserializes its {@code cooldown} from both
 * a plain JSON number (the skill editor's payload) and the evaluator-object
 * shape returned by {@code GET}, mirroring {@link SkillSerializer#parseCooldown}.
 */
class RequirementsDTODeserializerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void scalarCooldownDeserializesToConstantEvaluator() throws Exception {
        String json = "{\"cooldown\": 5, \"state\": [], \"items\": []}";
        RequirementsDTO dto = MAPPER.readValue(json, RequirementsDTO.class);

        assertNotNull(dto.cooldown());
        assertEquals("constant", dto.cooldown().type());
        assertEquals(5.0, dto.cooldown().params().get("value"));
    }

    @Test
    void objectCooldownDeserializes() throws Exception {
        String json = "{\"cooldown\": {\"type\": \"linear\", \"params\": {\"base\": 5.0, \"step\": -0.02, \"max\": 1.0}}, \"state\": [], \"items\": []}";
        RequirementsDTO dto = MAPPER.readValue(json, RequirementsDTO.class);

        assertNotNull(dto.cooldown());
        assertEquals("linear", dto.cooldown().type());
        assertEquals(5.0, dto.cooldown().params().get("base"));
    }

    @Test
    void zeroCooldownSavesAndLoads() throws Exception {
        String json = "{\"cooldown\": 0, \"state\": [], \"items\": []}";
        RequirementsDTO dto = MAPPER.readValue(json, RequirementsDTO.class);

        assertEquals("constant", dto.cooldown().type());
        assertEquals(0.0, dto.cooldown().params().get("value"));
    }

    @Test
    void stateAndItemsStillDeserialize() throws Exception {
        String json = "{\"cooldown\": 5, \"state\": [\"is_sneaking\"],"
                + "\"items\": [{\"action\": \"possession\", \"tag\": \"#minecraft:pickaxes\", \"slot\": \"MAIN_HAND\", \"amount\": 1}]}";
        RequirementsDTO dto = MAPPER.readValue(json, RequirementsDTO.class);

        assertEquals(5.0, dto.cooldown().params().get("value"));
        assertEquals(1, dto.state().size());
        assertEquals("is_sneaking", dto.state().get(0));
        assertEquals(1, dto.items().size());
        assertEquals("#minecraft:pickaxes", dto.items().get(0).tag());
    }
}
