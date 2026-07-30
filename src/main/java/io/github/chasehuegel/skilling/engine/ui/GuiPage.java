package io.github.chasehuegel.skilling.engine.ui;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable data for a single page in the paginated skills GUI.
 *
 * <p>Each page represents a category of skills displayed in a 54-slot chest
 * inventory. The {@code skillSlots} map assigns registered skill IDs to
 * specific inventory slots (0–53), excluding reserved navigation slots (45, 49, 53).
 *
 * @param id               unique page identifier matching the key in gui.yml
 * @param title            display name shown in the inventory title bar
 * @param icon             material string for the page indicator item (slot 49)
 * @param customModelData  optional custom model data for the page icon
 * @param skillSlots       map of skill-id → inventory slot (0–53)
 */
public record GuiPage(
        String id,
        String title,
        String icon,
        int customModelData,
        Map<String, Integer> skillSlots
) {
    public GuiPage {
        skillSlots = skillSlots != null ? Collections.unmodifiableMap(skillSlots) : Map.of();
    }
}
