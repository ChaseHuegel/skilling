package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Enhances fishing loot by applying a multiplier to caught item stacks
 * on {@link PlayerFishEvent}. Always activates on CAUGHT_FISH state.
 *
 * <p>A fractional result (e.g. 1 fish &times; 1.5) rounds up probabilistically, so
 * a small stack under a fractional multiplier yields its expected value instead
 * of flooring to nothing. The extra amount is dropped as a bonus, so the player
 * collects the multiplied total, never the original plus a full clone.
 *
 * <p><b>YAML key:</b> {@code core:fishing_loot}
 * <p><b>Required parameters:</b> {@code multiplier} (stack multiplier, e.g. 2.0 = double)
 */
public final class FishingLootMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble();

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * fraction roll; production always uses {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a value in [0, 1)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerFishEvent fishEvent)) return false;
        if (fishEvent.getState() != PlayerFishEvent.State.CAUGHT_FISH) return false;

        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;

        if (!(fishEvent.getCaught() instanceof Item caught)) return false;

        ItemStack stack = caught.getItemStack();
        int base = stack.getAmount();
        double scaled = base * multiplier;
        int newAmount = (int) scaled;
        // Round the fractional part up probabilistically: the average bonus over
        // many catches matches the configured multiplier (1.5 = +50% per catch).
        double fraction = scaled - newAmount;
        if (fraction > 0 && randomSource.getAsDouble() < fraction) {
            newAmount++;
        }
        if (newAmount > base) {
            int extra = newAmount - base;
            ItemStack bonus = stack.clone();
            bonus.setAmount(extra);
            player.getWorld().dropItemNaturally(caught.getLocation(), bonus);
        }
        return true;
    }
}
