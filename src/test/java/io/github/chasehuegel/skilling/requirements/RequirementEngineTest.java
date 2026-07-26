package io.github.chasehuegel.skilling.requirements;

import io.github.chasehuegel.skilling.engine.requirements.FailureReason;
import io.github.chasehuegel.skilling.engine.requirements.RequirementResult;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequirementEngineTest {

    @Test
    void passedResultObjectStructure() {
        var result = RequirementResult.PASSED;
        assertTrue(result.success());
        assertNull(result.failureReason());
    }

    @Test
    void failedResultContainsReasonAndPlaceholders() {
        var result = RequirementResult.failed(
                FailureReason.COOLDOWN,
                Map.of("time", "3.5")
        );
        assertFalse(result.success());
        assertEquals(FailureReason.COOLDOWN, result.failureReason());
        assertEquals("3.5", result.placeholders().get("time"));
    }

    @Test
    void itemRequirementRecord() {
        var itemReq = new SkillDefinition.ItemRequirement(
                "cost", "minecraft:coal", "MAIN_HAND", 1, 0.0
        );
        assertEquals("cost", itemReq.action());
        assertEquals("minecraft:coal", itemReq.tag());
        assertEquals("MAIN_HAND", itemReq.slot());
        assertEquals(1, itemReq.amount());
        assertEquals(0.0, itemReq.itemCooldown(), 1e-9);
    }

    @Test
    void requirementsRecordWithDefaults() {
        var req = new SkillDefinition.Requirements(
                5.0,
                List.of("is_sneaking"),
                List.of(new SkillDefinition.ItemRequirement("possession", "#minecraft:pickaxes", "MAIN_HAND", 1, 0.0))
        );
        assertEquals(5.0, req.cooldown(), 1e-9);
        assertTrue(req.state().contains("is_sneaking"));
        assertEquals(1, req.items().size());
    }
}