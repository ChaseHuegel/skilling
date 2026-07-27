package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import java.util.Map;

/**
 * Grants bonus furnace output items directly into the player's inventory on {@link FurnaceExtractEvent}.
 * Only activates when the multiplier is &gt; 1.0.
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
        int bonus = (int) Math.round(original * (multiplier - 1));
        if (bonus > 0) {
            var drops = extractEvent.getBlock().getDrops();
            if (!drops.isEmpty()) {
                player.getInventory().addItem(drops.iterator().next().asQuantity(bonus));
            }
        }
        return true;
    }
}
