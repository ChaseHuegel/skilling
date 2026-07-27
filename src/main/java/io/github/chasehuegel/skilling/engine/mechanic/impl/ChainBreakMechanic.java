package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.*;

/**
 * Breaks connected blocks of the same type up to a limit (vein mining).
 *
 * <p>YAML key: {@code core:chain_break}
 * <br>Parameters:
 * <ul>
 *   <li>{@code chain_limit} (double, cast to int) — maximum blocks to break</li>
 * </ul>
 * Filters: {@code target} material/tag
 */
public final class ChainBreakMechanic implements SkillMechanic {

    private static final int[][] DIRECTIONS = {
        {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
    };

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;
        int limit = ((Number) params.getOrDefault("chain_limit", 0.0)).intValue();
        if (limit <= 0) return false;

        double exhaustion = ((Number) params.getOrDefault("exhaustion", 0.0)).doubleValue();
        if (exhaustion > 0 && player.getFoodLevel() < exhaustion) return false;

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin.getLocation());

        int broken = 0;
        while (!queue.isEmpty() && broken < limit) {
            Block current = queue.poll();
            for (int[] dir : DIRECTIONS) {
                if (broken >= limit) break;
                Block neighbor = current.getRelative(dir[0], dir[1], dir[2]);
                Location loc = neighbor.getLocation();
                if (neighbor.getType() == targetType && !visited.contains(loc)) {
                    visited.add(loc);
                    neighbor.breakNaturally(player.getInventory().getItemInMainHand());
                    broken++;
                    if (broken < limit) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        if (exhaustion > 0) {
            int newFood = Math.max(0, player.getFoodLevel() - (int) Math.ceil(exhaustion));
            player.setFoodLevel(newFood);
        }

        return true;
    }
}
