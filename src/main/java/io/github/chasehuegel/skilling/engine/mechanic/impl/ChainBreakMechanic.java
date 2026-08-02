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
import org.bukkit.inventory.meta.Damageable;
import java.util.*;

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

    private static final Set<UUID> CHAINING_PLAYERS = new HashSet<>();
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
        return PROCESSING.get().contains(block.getLocation());
    }

    static void markChainProcessingForTest(Block block) {
        PROCESSING.get().add(block.getLocation());
    }

    static void clearChainProcessingForTest() {
        PROCESSING.remove();
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

        Set<Location> processing = PROCESSING.get();
        int[][] dirs = directions();
        try {
            int broken = 0;
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
                                damageTool(player);
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

    /**
     * Consumes 1 tool durability per chained block. The vanilla break only
     * deducts durability for the originating block, so the mechanic restores the
     * intended cost for every additional block it breaks.
     */
    private static void damageTool(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;
        if (tool.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            tool.setItemMeta((org.bukkit.inventory.meta.ItemMeta) damageable);
            player.getInventory().setItemInMainHand(tool);
        }
    }
}
