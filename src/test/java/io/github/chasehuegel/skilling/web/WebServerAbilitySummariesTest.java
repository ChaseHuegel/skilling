package io.github.chasehuegel.skilling.web;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link WebServer#abilitySummaries}: the registered ability summaries
 * expose each base ability's id and inherited identity (display name, trigger,
 * unlock level) with parse-style fallbacks, so the skill editor can display and
 * validate reference-shaped abilities.
 */
class WebServerAbilitySummariesTest {

    @Test
    void mapsRegisteredAbilitiesWithIdentityFields() {
        Map<String, Map<String, Object>> registry = new LinkedHashMap<>();
        registry.put("vein_miner", Map.of(
                "id", "vein_miner",
                "display_name", "Vein Miner",
                "trigger", "block_break",
                "unlock_level", 25));

        List<Map<String, Object>> summaries = WebServer.abilitySummaries(registry);
        assertEquals(1, summaries.size());
        Map<String, Object> summary = summaries.get(0);
        assertEquals("vein_miner", summary.get("id"));
        assertEquals("Vein Miner", summary.get("displayName"));
        assertEquals("block_break", summary.get("trigger"));
        assertEquals(25, summary.get("unlockLevel"));
    }

    @Test
    void appliesParseFallbacksForMissingFields() {
        Map<String, Map<String, Object>> registry = new LinkedHashMap<>();
        registry.put("bare", Map.of("id", "bare"));

        Map<String, Object> summary = WebServer.abilitySummaries(registry).get(0);
        assertEquals("bare", summary.get("displayName"), "display name falls back to the id");
        assertEquals("", summary.get("trigger"), "a missing trigger defaults to blank");
        assertEquals(1, summary.get("unlockLevel"), "a missing unlock level defaults to 1");
    }

    @Test
    void emptyRegistryProducesEmptyList() {
        assertTrue(WebServer.abilitySummaries(Map.of()).isEmpty());
    }
}
