package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Breaks connected blocks of the same material as the broken origin block,
 * expanding in all six directions (including up/down).
 *
 * <p>Expansion direction is configurable via the protected
 * {@link #directions()} hook so a plane-only variant ({@link LevelBreakMechanic})
 * reuses the same BFS, tool-damage, and guard logic.
 *
 * <p><b>YAML key:</b> {@code core:chain_break}
 * <br>Params: {@code chain_limit} (max total blocks broken including the origin)
 */
public class ChainBreakMechanic implements SkillMechanic {

    private static final Set<UUID> CHAINING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Set<Location>> PROCESSING =
            ThreadLocal.withInitial(HashSet::new);

    private static final int[][] DIRECTIONS = {
        {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
    };

    /**
     * Whether a block is currently being chain-broken, used by the event pipeline
     * to skip XP/ability processing for chained blocks (they were already handled
     * by the originating break).
     *
     * @param block the block being broken
     * @return true if the block is mid-chain-break
     */
    public static boolean isChainProcessing(Block block) {
        return processingSet().contains(block.getLocation());
    }

    /**
     * The set of locations currently being chain-broken on this thread. Exposed
     * package-private so tests can mark blocks as mid-chain without production
     * test-only methods; the event pipeline reads it through
     * {@link #isChainProcessing(Block)}.
     *
     * @return the live processing set for the current thread
     */
    static Set<Location> processingSet() {
        return PROCESSING.get();
    }

    /**
     * The neighbor offsets to expand into. Subclasses override to restrict the
     * expansion (e.g. XZ-plane only).
     *
     * @return an array of {@code {dx, dy, dz}} offsets
     */
    protected int[][] directions() {
        return DIRECTIONS;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;
        if (!CHAINING_PLAYERS.add(player.getUniqueId())) return false;
        int limit = ((Number) params.getOrDefault("chain_limit", 0.0)).intValue();
        if (limit <= 0) {
            CHAINING_PLAYERS.remove(player.getUniqueId());
            return false;
        }

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin.getLocation());

        Set<Location> processing = processingSet();
        int[][] dirs = directions();
        try {
            int broken = 0;
            search:
            while (!queue.isEmpty() && broken < limit) {
                Block current = queue.poll();
                for (int[] dir : dirs) {
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
                                // A broken tool must not keep applying silk-touch/
                                // fortune to later blocks for free drops.
                                if (ToolDurability.damageOnce(player,
                                        player.getInventory().getItemInMainHand())) {
                                    break search;
                                }
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

        return true;
    }
}
