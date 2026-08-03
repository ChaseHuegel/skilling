package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import java.util.Map;

/**
 * Increases the output amount of a crafted item by a multiplier on {@link CraftItemEvent}.
 * Only activates when the multiplier is &gt; 1.0.
 *
 * <p>For a normal craft the bonus is computed on the single recipe result. For a
 * shift-click the current amount already carries the batch-scaled total, so the
 * bonus is based on the underlying per-recipe result instead of being added on
 * top of the whole batch.
 *
 * <p>The result slot is capped at the material's max stack size; any bonus
 * overflow beyond one stack is granted as an extra stack in the player's
 * inventory (dropped on the ground if the inventory is full), so no bonus items
 * are ever lost.
 *
 * <p><b>YAML key:</b> {@code core:modify_craft_output}
 * <p><b>Optional parameters:</b> {@code multiplier} (default 1.0; extra output = amount &times; (multiplier - 1))
 */
public final class ModifyCraftOutputMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof CraftItemEvent craftEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;

        ItemStack result = craftEvent.getCurrentItem();
        if (result == null || result.isEmpty()) return false;

        int currentAmount = result.getAmount();
        int bonusBase = currentAmount;
        if (craftEvent.isShiftClick()) {
            // The current amount is the batch total; base the bonus on the
            // per-recipe result so it is not applied on top of the whole batch.
            int perRecipe = perRecipeAmount(craftEvent);
            if (perRecipe > 0) bonusBase = perRecipe;
        }

        int bonus = (int) Math.round(bonusBase * (multiplier - 1));
        if (bonus <= 0) return true;

        int maxStack = result.getType().getMaxStackSize();
        int total = currentAmount + bonus;
        int capped = Math.min(total, maxStack);
        if (capped > currentAmount) {
            result.setAmount(capped);
            craftEvent.setCurrentItem(result);
        }
        // The result slot holds at most one stack; grant any bonus overflow
        // beyond it so a shift-click batch never silently loses items.
        int overflow = total - capped;
        if (overflow > 0) {
            grantOverflow(player, result, overflow);
        }
        return true;
    }

    /**
     * Grants overflow bonus items beyond one stack as an extra stack in the
     * player's inventory, dropping on the ground when the inventory is full.
     */
    private static void grantOverflow(Player player, ItemStack result, int overflow) {
        ItemStack extra = result.clone();
        extra.setAmount(overflow);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(extra);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private static int perRecipeAmount(CraftItemEvent craftEvent) {
        Recipe recipe = craftEvent.getRecipe();
        if (recipe != null && recipe.getResult() != null) {
            return recipe.getResult().getAmount();
        }
        return 1;
    }
}
