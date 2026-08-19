package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Grants a bonus brewed potion on {@link BrewEvent} with a configured chance.
 *
 * <p>This is the brewing analogue of {@code core:yield_multiplier}: a chance
 * (0-100) that a finished batch yields an extra bottle. The bonus bottle clones
 * an existing brewed result and is placed in a free bottle slot of the brewing
 * stand; when all three slots are full it is granted to the player's inventory
 * (dropped if full), so no bonus item is ever lost.
 *
 * <p>Reaching the chance roll counts as an activation: the mechanic returns
 * {@code true} whether the roll succeeds or not, so the ability's shared
 * cost/cooldown is consumed once per finished batch and cannot be retried for
 * free. {@code false} is returned only when the mechanic cannot act (wrong
 * event type or an empty batch).
 *
 * <p><b>YAML key:</b> {@code core:modify_brew_output}
 * <br>Params: {@code chance} (0-100, default 0, percentage chance of an extra
 * bottle; 100 = always)
 */
public final class ModifyBrewOutputMechanic implements SkillMechanic {

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
        if (!(event instanceof BrewEvent brewEvent)) return false;

        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        BrewerInventory inventory = brewEvent.getContents();
        ItemStack model = findBrewedResult(inventory);
        if (model == null) return false;

        // A failed roll is still an activation attempt so the ability's
        // cost/cooldown is consumed once per batch and cannot be re-rolled free.
        if (chance < 100.0 && randomSource.getAsDouble() >= chance) return true;

        ItemStack bonus = model.clone();
        bonus.setAmount(1);
        if (!placeInFreeBottleSlot(inventory, bonus)) {
            grantToPlayer(player, bonus);
        }
        return true;
    }

    /**
     * Returns the first non-empty brewed bottle of the stand, or null when the
     * batch holds no brewed result (e.g. a failed brew).
     */
    private static ItemStack findBrewedResult(BrewerInventory inventory) {
        for (int slot = 0; slot < 3; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir() && item.getType() != Material.GLASS_BOTTLE) {
                return item;
            }
        }
        return null;
    }

    /**
     * Places {@code bonus} in the first free bottle slot of the stand.
     *
     * @return true if a free slot was found, false if the batch is full
     */
    private static boolean placeInFreeBottleSlot(BrewerInventory inventory, ItemStack bonus) {
        for (int slot = 0; slot < 3; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                inventory.setItem(slot, bonus);
                return true;
            }
        }
        return false;
    }

    private static void grantToPlayer(Player player, ItemStack bonus) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(bonus);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}
