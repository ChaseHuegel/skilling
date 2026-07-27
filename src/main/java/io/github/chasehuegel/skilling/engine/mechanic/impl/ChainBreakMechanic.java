package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.*;

public final class ChainBreakMechanic implements SkillMechanic {

    private static final Set<UUID> CHAINING_PLAYERS = new HashSet<>();
    private static final ThreadLocal<Set<Location>> PROCESSING =
            ThreadLocal.withInitial(HashSet::new);

    private static final int[][] DIRECTIONS = {
        {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
    };

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;
        if (!CHAINING_PLAYERS.add(player.getUniqueId())) return false;
        int limit = ((Number) params.getOrDefault("chain_limit", 0.0)).intValue();
        if (limit <= 0) {
            CHAINING_PLAYERS.remove(player.getUniqueId());
            return false;
        }

        double exhaustion = ((Number) params.getOrDefault("exhaustion", 0.0)).doubleValue();
        if (exhaustion > 0 && player.getFoodLevel() < exhaustion) {
            CHAINING_PLAYERS.remove(player.getUniqueId());
            return false;
        }

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin.getLocation());

        Set<Location> processing = PROCESSING.get();
        try {
            int broken = 0;
            while (!queue.isEmpty() && broken < limit) {
                Block current = queue.poll();
                for (int[] dir : DIRECTIONS) {
                    if (broken >= limit) break;
                    Block neighbor = current.getRelative(dir[0], dir[1], dir[2]);
                    Location loc = neighbor.getLocation();
                    if (neighbor.getType() == targetType && !visited.contains(loc)
                            && !processing.contains(loc)) {
                        visited.add(loc);
                        processing.add(loc);
                        try {
                            BlockBreakEvent chainEvent = new BlockBreakEvent(neighbor, player);
                            Bukkit.getPluginManager().callEvent(chainEvent);
                            if (!chainEvent.isCancelled()) {
                                neighbor.breakNaturally(player.getInventory().getItemInMainHand());
                                broken++;
                                if (broken < limit) {
                                    queue.add(neighbor);
                                }
                            }
                        } finally {
                            processing.remove(loc);
                        }
                    }
                }
            }
        } finally {
            CHAINING_PLAYERS.remove(player.getUniqueId());
            if (processing.isEmpty()) {
                PROCESSING.remove();
            }
        }

        if (exhaustion > 0) {
            int newFood = Math.max(0, player.getFoodLevel() - (int) Math.ceil(exhaustion));
            player.setFoodLevel(newFood);
        }

        return true;
    }
}
