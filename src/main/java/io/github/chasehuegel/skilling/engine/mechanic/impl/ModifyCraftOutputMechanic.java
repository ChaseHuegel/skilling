package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Increases the output amount of a crafted item by a multiplier on {@link CraftItemEvent}.
 * Only activates when the multiplier is &gt; 1.0.
 *
 * <p><b>YAML key:</b> {@code modify_craft_output}
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

        int bonus = (int) Math.round(result.getAmount() * (multiplier - 1));
        if (bonus > 0) {
            result.setAmount(result.getAmount() + bonus);
            craftEvent.setCurrentItem(result);
        }
        return true;
    }
}
