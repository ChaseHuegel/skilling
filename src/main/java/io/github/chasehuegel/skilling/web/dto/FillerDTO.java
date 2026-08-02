package io.github.chasehuegel.skilling.web.dto;

/**
 * Global filler configuration for unassigned GUI slots.
 *
 * @param material         material string (e.g. "minecraft:black_stained_glass_pane")
 * @param customModelData  optional custom model data (0 = none)
 */
public record FillerDTO(String material, int customModelData) {

    /** The plugin's default filler used when none is configured. */
    public static final FillerDTO DEFAULT = new FillerDTO("minecraft:black_stained_glass_pane", 0);
}
