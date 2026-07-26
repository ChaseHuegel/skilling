package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import java.util.Map;

public final class ModifyFurnaceOutputMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof FurnaceExtractEvent extractEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;

        int original = extractEvent.getItemAmount();
        int bonus = (int) Math.round(original * (multiplier - 1));
        if (bonus > 0) {
            player.getInventory().addItem(
                    extractEvent.getBlock().getDrops().iterator().next().asQuantity(bonus)
            );
        }
        return true;
    }
}
