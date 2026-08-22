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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Breaks blocks in a radius around the originally broken block on {@link BlockBreakEvent}.
 * Only breaks blocks matching the original block's type. The radius expands horizontally
 * (X/Z plane) from the origin and is clamped to a bounded maximum; the scan stops once
 * {@code max_blocks} is reached. Each harvested block goes through a synthetic
 * {@link BlockBreakEvent} so region/protection plugins can cancel it. Ageable
 * crops still growing (not at maximum age) are skipped, so a farm harvest never
 * clears immature plants; non-ageable targets are unaffected.
 *
 * <p><b>YAML key:</b> {@code core:area_harvest}
 * <p><b>Required parameters:</b> {@code radius} (Manhattan radius, 0 = single block, 1 = 3x3, 2 = 5x5; clamped to 32)
 * <p><b>Optional parameters:</b> {@code max_blocks} (max blocks to break, default 64)
 */
public final class AreaHarvestMechanic implements SkillMechanic {

    static final int MAX_RADIUS = 32;
    static final int DEFAULT_MAX_BLOCKS = 64;

    /**
     * Upper bound on {@code max_blocks}: a larger value (e.g. from a level-scaled
     * evaluator) would break hundreds of thousands of blocks synchronously on the
     * main thread and freeze the server, so the budget is clamped here.
     */
    static final int MAX_BLOCKS = 128;

    private static final ThreadLocal<Set<Location>> PROCESSING =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * Whether a block is currently being area-harvested, so the event pipeline skips
     * re-processing (XP/abilities) for harvested blocks.
     *
     * @param block the block being harvested
     * @return true if the block is mid-harvest
     */
    public static boolean isChainProcessing(Block block) {
        return processingSet().contains(block.getLocation());
    }

    /**
     * The set of locations currently being harvested on this thread. Exposed
     * package-private so tests can mark blocks as mid-harvest without production
     * test-only methods; the event pipeline reads it through
     * {@link #isChainProcessing(Block)}.
     *
     * @return the live processing set for the current thread
     */
    static Set<Location> processingSet() {
        return PROCESSING.get();
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;

        int radius = ((Number) params.getOrDefault("radius", 1.0)).intValue();
        if (radius <= 0) return false;
        radius = Math.min(radius, MAX_RADIUS);

        int maxBlocks = ((Number) params.getOrDefault("max_blocks", DEFAULT_MAX_BLOCKS)).intValue();
        if (maxBlocks <= 0) return false;
        maxBlocks = Math.min(maxBlocks, MAX_BLOCKS);

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Set<Location> processing = processingSet();
        try {
            int broken = 0;
            for (int dx = -radius; dx <= radius && broken < maxBlocks; dx++) {
                for (int dz = -radius; dz <= radius && broken < maxBlocks; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block neighbor = origin.getRelative(dx, 0, dz);
                    if (neighbor.getType() != targetType) continue;
                    // Skip crops still growing: harvesting must not clear an
                    // immature patch. Non-ageable targets (stone, dirt, logs, or
                    // always-ready fruit blocks) have no growth stage and pass.
                    if (!CropMaturity.isMature(neighbor)) continue;
                    Location loc = neighbor.getLocation();
                    if (processing.contains(loc)) continue;
                    processing.add(loc);
                    try {
                        // Respect region/protection plugins: a cancelled harvest
                        // event leaves the block untouched.
                        BlockBreakEvent harvestEvent = new BlockBreakEvent(neighbor, player);
                        Bukkit.getPluginManager().callEvent(harvestEvent);
                        if (harvestEvent.isCancelled()) continue;
                        neighbor.breakNaturally(tool);
                        broken++;
                        // The vanilla break only deducted durability for the
                        // origin block; each additional harvested block costs one.
                        if (ToolDurability.damageOnce(player, tool)) {
                            // The tool broke mid-harvest; stop so a broken tool
                            // cannot grant free drops on remaining blocks.
                            return true;
                        }
                    } finally {
                        processing.remove(loc);
                    }
                }
            }
        } finally {
            if (processing.isEmpty()) {
                PROCESSING.remove();
            }
        }
        return true;
    }
}
