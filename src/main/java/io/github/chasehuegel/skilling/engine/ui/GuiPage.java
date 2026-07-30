package io.github.chasehuegel.skilling.engine.ui;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable data for a single page in the paginated skills GUI.
 *
 * <p>Each page represents a category of skills displayed in a chest
 * inventory. The {@code skillSlots} map assigns registered skill IDs to
 * specific inventory slots. Navigation (arrows + page indicator) occupies
 * the last row automatically.
 *
 * @param id               unique page identifier matching the key in gui.yml
 * @param title            display name for the page indicator item
 * @param guiTitle         inventory title bar text (optional, supports {@code &} color codes; falls back to {@code title})
 * @param icon             material string for the page indicator item
 * @param customModelData  optional custom model data for the page icon
 * @param rows             number of inventory rows (0 = full 6-row / 54-slot, 1–6 = custom)
 * @param skillSlots       map of skill-id → inventory slot
 */
public record GuiPage(
        String id,
        String title,
        String guiTitle,
        String icon,
        int customModelData,
        int rows,
        Map<String, Integer> skillSlots
) {
    public GuiPage {
        skillSlots = skillSlots != null ? Collections.unmodifiableMap(skillSlots) : Map.of();
    }

    /**
     * Returns the effective inventory title, using {@code guiTitle} when
     * set and non-blank, falling back to {@code title}.
     *
     * @return the display title for the inventory window
     */
    public String displayTitle() {
        return guiTitle != null && !guiTitle.isBlank() ? guiTitle : title;
    }

    /**
     * Returns the inventory size for this page.
     * When rows is 0, defaults to 54 (full chest).
     *
     * @return slot count for the inventory
     */
    public int inventorySize() {
        return rows > 0 ? rows * 9 : 54;
    }

    /**
     * Returns the effective row count for this page.
     * When rows is 0, returns 6 (full chest).
     *
     * @return effective row count
     */
    public int effectiveRows() {
        return rows > 0 ? rows : 6;
    }

    /**
     * Returns the slot index of the previous-page arrow (first column of last row).
     *
     * @return the previous-page arrow slot
     */
    public int prevSlot() {
        return (effectiveRows() - 1) * 9;
    }

    /**
     * Returns the slot index of the page indicator (center column of last row).
     *
     * @return the page indicator slot
     */
    public int indicatorSlot() {
        return (effectiveRows() - 1) * 9 + 4;
    }

    /**
     * Returns the slot index of the next-page arrow (last column of last row).
     *
     * @return the next-page arrow slot
     */
    public int nextSlot() {
        return (effectiveRows() - 1) * 9 + 8;
    }
}
