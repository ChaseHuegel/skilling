package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grants bonus furnace output items directly into the player's inventory on {@link FurnaceExtractEvent}.
 * Only activates when the multiplier is &gt; 1.0.
 * Overflow items that don't fit in the inventory are dropped at the player's feet.
 *
 * <p><b>YAML key:</b> {@code core:modify_furnace_output}
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
            // furnace block's drops. Split it into capped stacks so a large
            // multiplier can never produce an oversized ItemStack.
            for (ItemStack bonusItem : splitBonus(extractEvent.getItemType(), bonus)) {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(bonusItem);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
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

    /**
     * Splits a bonus quantity into stacks capped at the item's max stack size,
     * mirroring {@link YieldMultiplierMechanic}'s split loop so a large
     * multiplier never produces an oversized {@link ItemStack}.
     *
     * @param itemType the smelted product type
     * @param bonus    the total bonus quantity to grant
     * @return capped stacks whose amounts sum to {@code bonus}
     */
    public static List<ItemStack> splitBonus(Material itemType, int bonus) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int amount : splitAmounts(itemType.getMaxStackSize(), bonus)) {
            stacks.add(new ItemStack(itemType, amount));
        }
        return stacks;
    }

    /**
     * Computes the capped stack sizes that sum to {@code bonus}, so the caller
     * never has to construct an oversized {@link ItemStack}.
     *
     * @param maxStackSize the item's stack-size cap
     * @param bonus        the total bonus quantity
     * @return the capped amounts, largest first
     */
    public static List<Integer> splitAmounts(int maxStackSize, int bonus) {
        List<Integer> amounts = new ArrayList<>();
        int remaining = bonus;
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStackSize);
            amounts.add(amount);
            remaining -= amount;
        }
        return amounts;
    }
}
