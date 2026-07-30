package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.Map;

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
