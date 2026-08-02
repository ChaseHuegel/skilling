package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Grants bonus furnace output items directly into the player's inventory on {@link FurnaceExtractEvent}.
 * Only activates when the multiplier is &gt; 1.0.
 * Overflow items that don't fit in the inventory are dropped at the player's feet.
 *
 * <p><b>YAML key:</b> {@code modify_furnace_output}
 * <p><b>Optional parameters:</b> {@code multiplier} (default 1.0; bonus items = original &times; (multiplier - 1))
 */
public final class ModifyFurnaceOutputMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof FurnaceExtractEvent extractEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;

        int original = extractEvent.getItemAmount();
        int bonus = computeBonus(original, multiplier);
        if (bonus > 0) {
            // The bonus must be the smelted product (getItemType), never the
            // furnace block's drops.
            ItemStack bonusItem = new ItemStack(extractEvent.getItemType(), bonus);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(bonusItem);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        return true;
    }

    /**
     * Computes the bonus quantity for a furnace extraction.
     *
     * @param original   the number of items extracted
     * @param multiplier the configured output multiplier
     * @return the extra items to grant (original × (multiplier − 1))
     */
    public static int computeBonus(int original, double multiplier) {
        return (int) Math.round(original * (multiplier - 1));
    }
}
