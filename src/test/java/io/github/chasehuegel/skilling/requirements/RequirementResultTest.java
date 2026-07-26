package io.github.chasehuegel.skilling.requirements;

import io.github.chasehuegel.skilling.engine.requirements.FailureReason;
import io.github.chasehuegel.skilling.engine.requirements.RequirementResult;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequirementResultTest {

    @Test
    void passedResultIsSuccessful() {
        assertTrue(RequirementResult.PASSED.success());
        assertNull(RequirementResult.PASSED.failureReason());
        assertTrue(RequirementResult.PASSED.placeholders().isEmpty());
    }

    @Test
    void failedResultHasFailureReason() {
        var result = RequirementResult.failed(FailureReason.COOLDOWN, Map.of("time", "5.0"));
        assertFalse(result.success());
        assertEquals(FailureReason.COOLDOWN, result.failureReason());
        assertEquals("5.0", result.placeholders().get("time"));
    }

    @Test
    void failureReasonsAreDistinct() {
        assertNotEquals(FailureReason.COOLDOWN, FailureReason.MISSING_ITEM);
        assertNotEquals(FailureReason.MISSING_STATE, FailureReason.INSUFFICIENT_ITEMS);
    }

    @Test
    void customPlaceholders() {
        var result = RequirementResult.failed(
                FailureReason.MISSING_ITEM,
                Map.of("item", "minecraft:diamond", "amount", "3")
        );
        assertEquals("minecraft:diamond", result.placeholders().get("item"));
        assertEquals("3", result.placeholders().get("amount"));
    }
}