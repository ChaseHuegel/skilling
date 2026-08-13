package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
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
 * <p>The optional {@code target} parameter restricts the chain to a material or
 * tag reference ({@code #minecraft:logs}, {@code #c:logs}, or
 * {@code minecraft:oak_log}) instead of the origin block's own material, so a
 * single ability can fell a tree by chaining the tagged logs and every connected
 * leaves block. The target is resolved once per execution through the live
 * plugin {@link TagResolver} ({@link Skilling#getTagResolver()}), which flattens
 * tag references into a cached {@link EnumSet} at load; the set is used as an
 * O(1) per-neighbor membership test, so no tag resolution happens per block.
 *
 * <p><b>YAML key:</b> {@code core:chain_break}
 * <br>Params: {@code chain_limit} (max total blocks broken including the origin),
 * {@code target} (optional; a material or tag reference restricting the chain,
 * defaults to the origin block's own material)
 */
public class ChainBreakMechanic implements SkillMechanic {

    /**
     * Upper bound on {@code chain_limit}: a larger value (e.g. from a level-scaled
     * evaluator) would break hundreds of thousands of blocks synchronously on the
     * main thread and freeze the server, so the budget is clamped here.
     */
    static final int MAX_CHAIN_LIMIT = 128;

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

    /**
     * Resolves the optional {@code target} reference into an {@link EnumSet} of
     * materials, or null when the parameter is absent/blank (chain to the origin
     * block's own material). Resolution is delegated to the live plugin
     * {@link TagResolver}, which caches the flattened set so repeated
     * executions cost an O(1) map lookup instead of re-reading tag data.
     *
     * @param params the evaluated mechanic parameters
     * @return the flattened target material set, or null for origin-material chaining
     */
    private static Set<Material> resolveTarget(Map<String, Object> params) {
        Object raw = params.get("target");
        if (raw == null) return null;
        String reference = String.valueOf(raw);
        if (reference.isBlank()) return null;
        TagResolver resolver = Skilling.getInstance().getTagResolver();
        if (resolver == null) {
            throw new IllegalStateException("Cannot resolve chain_break 'target' without a live TagResolver");
        }
        return resolver.resolve(reference);
    }

    /**
     * Whether a neighbor block should be chained: with a target set the neighbor
     * must be a member of it, otherwise it must match the origin block's material.
     *
     * @param neighbor   the candidate neighbor block
     * @param targetType the origin block's material (used when no target is set)
     * @param targetSet  the flattened target set, or null for origin-material chaining
     * @return true if the neighbor chains
     */
    private static boolean matches(Block neighbor, Material targetType, Set<Material> targetSet) {
        return targetSet == null ? neighbor.getType() == targetType : targetSet.contains(neighbor.getType());
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
        limit = Math.min(limit, MAX_CHAIN_LIMIT);

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();
        Set<Material> targetSet = resolveTarget(params);
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin.getLocation());

        Set<Location> processing = processingSet();
        int[][] dirs = directions();
        try {
            // chain_limit counts the origin block, which is broken by the
            // originating BlockBreakEvent rather than this mechanic, so the
            // mechanic may break at most limit - 1 chained blocks.
            int chainedBudget = limit - 1;
            int broken = 0;
            search:
            while (!queue.isEmpty() && broken < chainedBudget) {
                Block current = queue.poll();
                for (int[] dir : dirs) {
                    if (broken >= chainedBudget) break;
                    Block neighbor = current.getRelative(dir[0], dir[1], dir[2]);
                    Location loc = neighbor.getLocation();
                    if (matches(neighbor, targetType, targetSet) && !visited.contains(loc)
                            && !processing.contains(loc)) {
                        visited.add(loc);
                        processing.add(loc);
                        try {
                            BlockBreakEvent chainEvent = new BlockBreakEvent(neighbor, player);
                            Bukkit.getPluginManager().callEvent(chainEvent);
                            // A failed break (e.g. an unbreakable same-type block)
                            // consumes neither the chain limit nor tool durability.
                            if (!chainEvent.isCancelled()
                                    && neighbor.breakNaturally(player.getInventory().getItemInMainHand())) {
                                broken++;
                                // A broken tool must not keep applying silk-touch/
                                // fortune to later blocks for free drops.
                                if (ToolDurability.damageOnce(player,
                                        player.getInventory().getItemInMainHand())) {
                                    break search;
                                }
                                if (broken < chainedBudget) {
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
