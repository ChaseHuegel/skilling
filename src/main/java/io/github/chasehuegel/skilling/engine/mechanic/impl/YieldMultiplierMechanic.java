package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Duplicates block drops with a percentage chance on {@link BlockBreakEvent}. When triggered, the
 * block's natural drops are doubled and dropped once at the block location, replacing the vanilla
 * drops (the vanilla drop pipeline is suppressed so a break never yields original + doubled = 3x).
 *
 * <p>An optional {@code triple_chance} rolls first: when it succeeds the drops are tripled instead
 * of doubled, so a single mechanic expresses "always double, sometimes triple" capstones without
 * two stacked mechanics multiplying the same pre-break drops into a quadruple yield.
 *
 * <p><b>YAML key:</b> {@code core:yield_multiplier}
 * <p><b>Required parameters:</b> {@code yield_chance} (0-100, percentage chance to double drops)
 * <p><b>Optional parameters:</b> {@code triple_chance} (0-100, percentage chance to triple instead)
 */
public final class YieldMultiplierMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * roll; production always uses {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;
        double chance = ((Number) params.getOrDefault("yield_chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        double tripleChance = ((Number) params.getOrDefault("triple_chance", 0.0)).doubleValue();

        int multiplier = 2;
        if (tripleChance > 0 && randomSource.getAsDouble() < tripleChance) {
            multiplier = 3;
        } else if (randomSource.getAsDouble() >= chance) {
            // Reaching the roll counts as an activation attempt even when the
            // double roll fails (consume-once semantics, never a free retry).
            return true;
        }

        // Suppress the vanilla drops so the multiplied set below is the only loot.
        breakEvent.setDropItems(false);
        Collection<ItemStack> drops = breakEvent.getBlock().getDrops(player.getInventory().getItemInMainHand());
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            // Clamp each multiplied stack to the stack-size cap, dropping the
            // remainder as an extra stack so a doubling never exceeds 64.
            int remaining = drop.getAmount() * multiplier;
            int maxStack = drop.getMaxStackSize();
            var location = breakEvent.getBlock().getLocation().add(0.5, 0.5, 0.5);
            while (remaining > 0) {
                ItemStack toDrop = drop.clone();
                toDrop.setAmount(Math.min(remaining, maxStack));
                breakEvent.getBlock().getWorld().dropItemNaturally(location, toDrop);
                remaining -= Math.min(remaining, maxStack);
            }
        }

        return true;
    }
}
