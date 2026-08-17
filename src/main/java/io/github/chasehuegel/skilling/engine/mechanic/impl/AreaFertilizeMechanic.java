package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFertilizeEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Spreads bonemeal to matching blocks around the block a player fertilizes on
 * {@link BlockFertilizeEvent}.
 *
 * <p>When a player uses bonemeal, every same-type block in the radius around the
 * fertilized block is grown too, so one bonemeal feeds a small patch instead of
 * a single crop. The scan stays in the X/Z plane and is clamped to a bounded
 * radius, keeping the work O(radius) and event-driven with no per-tick cost.
 * Bonemeal remains the required catalyst; vanilla growth rules still apply.
 *
 * <p>A ThreadLocal guard keeps the nested {@link BlockFertilizeEvent} that
 * {@code applyBoneMeal} raises from re-entering the ability pipeline, so the
 * spread cannot cascade recursively.
 *
 * <p><b>YAML key:</b> {@code core:area_fertilize}
 * <p><b>Required parameters:</b> {@code radius} (Manhattan radius, 0 = single
 * block, 1 = 3x3, 2 = 5x5; clamped to 8)
 */
public final class AreaFertilizeMechanic implements SkillMechanic {

    /**
     * Upper bound on the fertilize radius: a larger value (e.g. from a
     * level-scaled evaluator) would grow hundreds of blocks synchronously on the
     * main thread, so the radius is clamped here.
     */
    static final int MAX_RADIUS = 8;

    private static final ThreadLocal<Set<Location>> PROCESSING =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * Whether a block is currently being area-fertilized, so the fertilize
     * listener skips the nested {@link BlockFertilizeEvent} raised by
     * {@code applyBoneMeal} and the spread cannot cascade recursively.
     *
     * @param block the block being fertilized
     * @return true if the block is mid-fertilization
     */
    public static boolean isFertilizeProcessing(Block block) {
        return processingSet().contains(block.getLocation());
    }

    /**
     * The set of locations currently being fertilized on this thread. Exposed
     * package-private so tests can mark blocks as mid-fertilize without
     * production test-only methods; the fertilize listener reads it through
     * {@link #isFertilizeProcessing(Block)}.
     *
     * @return the live processing set for the current thread
     */
    static Set<Location> processingSet() {
        return PROCESSING.get();
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockFertilizeEvent fertilizeEvent)) return false;
        if (fertilizeEvent.getPlayer() == null) return false;

        int radius = ((Number) params.getOrDefault("radius", 1.0)).intValue();
        if (radius <= 0) return false;
        radius = Math.min(radius, MAX_RADIUS);

        Block origin = fertilizeEvent.getBlock();
        Material targetType = origin.getType();
        Set<Location> processing = processingSet();
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block neighbor = origin.getRelative(dx, 0, dz);
                    if (neighbor.getType() != targetType) continue;
                    Location loc = neighbor.getLocation();
                    if (processing.contains(loc)) continue;
                    processing.add(loc);
                    try {
                        neighbor.applyBoneMeal(BlockFace.UP);
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
