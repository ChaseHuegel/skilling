package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Multiplies natural yield: block drops on {@link BlockBreakEvent} and mob drops
 * on {@link EntityDeathEvent}, each with a percentage chance.
 *
 * <p><b>Block path:</b> When the chance roll succeeds, the block's natural drops
 * are doubled and dropped once at the block location, replacing the vanilla drops
 * (the vanilla drop pipeline is suppressed so a break never yields original +
 * doubled = 3x).
 *
 * <p><b>Mob path:</b> When the chance roll succeeds, the {@code EntityDeathEvent}'s
 * drops are multiplied in place (an extra copy of each drop is appended respecting
 * the stack-size cap), so a butcher-style skill doubles the animal's own loot. The
 * natural drops remain the source; the mechanic amplifies them rather than
 * replacing the drop table.
 *
 * <p>An optional {@code triple_chance} rolls first on either path: when it succeeds
 * the drops are tripled instead of doubled, so a single mechanic expresses
 * "always double, sometimes triple" capstones without two stacked mechanics
 * multiplying the same base yield into a quadruple.
 *
 * <p>Reaching the roll counts as an activation attempt whether or not it lands, so
 * a failed roll cannot be retried for free (consume-once semantics).
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

        if (event instanceof BlockBreakEvent breakEvent) {
            return amplifyBlockDrops(player, breakEvent, multiplier);
        }
        if (event instanceof EntityDeathEvent deathEvent) {
            return amplifyEntityDrops(deathEvent, multiplier);
        }
        return false;
    }

    /**
     * Doubles (or triples) the block's natural drops, suppressing the vanilla
     * drop pipeline so the multiplied set below is the only loot.
     *
     * @param breakEvent the block break event
     * @param multiplier the drop multiplier (2 or 3)
     * @return true
     */
    private static boolean amplifyBlockDrops(Player player, BlockBreakEvent breakEvent, int multiplier) {
        breakEvent.setDropItems(false);
        Collection<ItemStack> drops = breakEvent.getBlock().getDrops(player.getInventory().getItemInMainHand());
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
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

    /**
     * Stacks extra copies of each natural mob drop in the event's drop list so
     * the spawned loot is multiplied without replacing the vanilla drop table.
     * The drops are appended once (not multiplied in place) so the original item
     * stays intact and each extra copy respects the stack-size cap.
     *
     * @param deathEvent the entity death event
     * @param multiplier the drop multiplier (2 or 3)
     * @return true
     */
    private static boolean amplifyEntityDrops(EntityDeathEvent deathEvent, int multiplier) {
        List<ItemStack> drops = deathEvent.getDrops();
        if (drops == null || drops.isEmpty()) return true;
        List<ItemStack> originals = List.copyOf(drops);
        for (ItemStack drop : originals) {
            if (drop == null || drop.isEmpty()) continue;
            int extras = drop.getAmount() * (multiplier - 1);
            int maxStack = drop.getMaxStackSize();
            while (extras > 0) {
                ItemStack copy = drop.clone();
                copy.setAmount(Math.min(extras, maxStack));
                drops.add(copy);
                extras -= Math.min(extras, maxStack);
            }
        }
        return true;
    }
}