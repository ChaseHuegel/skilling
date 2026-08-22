package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

/**
 * Shared crop-maturity check for farmers and the {@code grown} state filter.
 *
 * <p>Farming content must never reward or act on immature crops: a freshly
 * planted seed breaks for a vanilla seed drop (seed-neutral), so rewarding an
 * immature break opens an infinite place+break loop for both XP and yield
 * doubling. This helper gats such content to harvest-ready plants:
 * <ul>
 *   <li>An {@link Ageable} block (wheat, carrot, potato, beetroot, cocoa) is
 *       harvest-ready only at its maximum age.</li>
 *   <li>A non-ageable block (melon fruit, pumpkin fruit, sugar cane, but also
 *       stone, dirt, and logs) has no progress stage, so it is always
 *       harvest-ready. These blocks become breakable only when the plant around
 *       them matures, which is itself time-gated.</li>
 * </ul>
 */
public final class CropMaturity {

    private CropMaturity() {
    }

    /**
     * Whether a block is harvest-ready: an {@link Ageable} block at maximum age,
     * or any non-ageable block. Non-ageable blocks never have a progress stage,
     * so they are always considered mature.
     *
     * @param block the block to inspect
     * @return true if the block is mature
     */
    public static boolean isMature(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }
}