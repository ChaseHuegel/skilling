package io.github.chasehuegel.skilling.web.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for the gui.yml configuration file.
 * Defines the skill overview chest GUI layout: title, row count, and named pages
 * with slot-to-skill-id mappings.
 *
 * @param title   The chest GUI title (supports MiniMessage &amp; color codes)
 * @param rows    Number of chest rows (3-6)
 * @param pages   Ordered list of page definitions
 * @param version Schema version for future migration support
 */
public record GuiLayoutDTO(
    String title,
    int rows,
    List<GuiPageDTO> pages,
    int version
) {
    public static GuiLayoutDTO empty() {
        return new GuiLayoutDTO(
            "&8\u2692 &6Skills &8\u2692",
            6,
            List.of(new GuiPageDTO("&6Skills", Collections.emptyMap())),
            1
        );
    }

    public record GuiPageDTO(
        String label,
        Map<Integer, String> slots
    ) {}
}
