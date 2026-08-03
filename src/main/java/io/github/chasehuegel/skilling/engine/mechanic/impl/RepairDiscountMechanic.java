package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import java.util.Map;

/**
 * Reduces the experience level cost of anvil repairs on {@link PrepareAnvilEvent}
 * by a percentage discount, floored at 1 level.
 *
 * <p>Only the repairing player's own anvil view is discounted; a discount of
 * {@code 0} or less is a no-op.
 *
 * <p><b>YAML key:</b> {@code core:repair_discount}
 * <br>Params: {@code discount} (0-100, percentage discount)
 */
public final class RepairDiscountMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PrepareAnvilEvent anvilEvent)) return false;
        double discount = ((Number) params.getOrDefault("discount", 0.0)).doubleValue();
        if (discount <= 0) return false;
        AnvilView view = anvilEvent.getView();
        if (!(view.getPlayer() instanceof Player anvilPlayer) || !anvilPlayer.equals(player)) return false;
        int baseCost = view.getRepairCost();
        if (baseCost <= 0) return false;
        int newCost = Math.max(1, (int) Math.round(baseCost * (1.0 - discount / 100.0)));
        view.setRepairCost(newCost);
        return true;
    }
}
