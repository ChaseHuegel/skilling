package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.Map;

/**
 * Reduces the experience level cost of enchanting on {@link EnchantItemEvent}
 * by a percentage discount, floored at 1 level.
 *
 * <p>Only the enchanting player's own table is discounted; a discount of
 * {@code 0} or less is a no-op.
 *
 * <p><b>YAML key:</b> {@code core:modify_enchant_cost}
 * <br>Params: {@code discount} (0-100, percentage discount)
 */
public final class ModifyEnchantCostMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EnchantItemEvent enchantEvent)) return false;
        if (!enchantEvent.getEnchanter().equals(player)) return false;
        double discount = ((Number) params.getOrDefault("discount", 0.0)).doubleValue();
        if (discount <= 0) return false;
        int oldLevelCost = enchantEvent.getExpLevelCost();
        int newLevelCost = Math.max(1, (int) Math.round(oldLevelCost * (1.0 - discount / 100.0)));
        enchantEvent.setExpLevelCost(newLevelCost);
        return true;
    }
}
