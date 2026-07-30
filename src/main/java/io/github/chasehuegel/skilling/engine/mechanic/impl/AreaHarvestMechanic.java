package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.Map;

/**
 * Breaks blocks in a radius around the originally broken block on {@link BlockBreakEvent}.
 * Only breaks blocks matching the original block's type. The radius expands horizontally
 * (X/Z plane) from the origin, limited to a configurable number of total blocks.
 *
 * <p><b>YAML key:</b> {@code core:area_harvest}
 * <p><b>Required parameters:</b> {@code radius} (Manhattan radius, 0 = single block, 1 = 3x3, 2 = 5x5)
 * <p><b>Optional parameters:</b> {@code max_blocks} (max blocks to break, default unlimited)
 */
public final class AreaHarvestMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;

        int radius = ((Number) params.getOrDefault("radius", 1.0)).intValue();
        if (radius <= 0) return false;

        int maxBlocks = ((Number) params.getOrDefault("max_blocks", Integer.MAX_VALUE)).intValue();

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();

        int broken = 0;
        for (int dx = -radius; dx <= radius && broken < maxBlocks; dx++) {
            for (int dz = -radius; dz <= radius && broken < maxBlocks; dz++) {
                if (dx == 0 && dz == 0) continue;
                Block neighbor = origin.getRelative(dx, 0, dz);
                if (neighbor.getType() != targetType) continue;
                Location loc = neighbor.getLocation();
                neighbor.breakNaturally(player.getInventory().getItemInMainHand());
                broken++;
            }
        }
        return true;
    }
}
