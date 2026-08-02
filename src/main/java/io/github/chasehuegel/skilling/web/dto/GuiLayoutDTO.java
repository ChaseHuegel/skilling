package io.github.chasehuegel.skilling.web.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for the gui.yml configuration file.
 * Defines the skill overview chest GUI layout: title, row count, named pages
 * with slot-to-skill-id mappings, schema version, and global filler.
 *
 * @param title   The chest GUI title (supports MiniMessage &amp; color codes)
 * @param rows    Number of chest rows (1-6)
 * @param pages   Ordered list of page definitions
 * @param version Schema version for future migration support
 * @param filler  Global filler for unassigned slots
 */
public record GuiLayoutDTO(
    String title,
    int rows,
    List<GuiPageDTO> pages,
    int version,
    FillerDTO filler
) {
    public GuiLayoutDTO {
        if (filler == null) filler = FillerDTO.DEFAULT;
    }

    public GuiLayoutDTO(String title, int rows, List<GuiPageDTO> pages, int version) {
        this(title, rows, pages, version, FillerDTO.DEFAULT);
    }

    public static GuiLayoutDTO empty() {
        return new GuiLayoutDTO(
            "&8\u2692 &6Skills &8\u2692",
            6,
            List.of(new GuiPageDTO("&6Skills", Collections.emptyMap(), "minecraft:book", 0)),
            1
        );
    }

    public record GuiPageDTO(
        String label,
        Map<Integer, String> slots,
        String icon,
        int customModelData
    ) {
        public GuiPageDTO {
            if (icon == null) icon = "minecraft:book";
        }
    }
}
