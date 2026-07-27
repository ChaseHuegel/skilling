package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.BrewEvent;
import java.util.Map;

/**
 * Modifies the remaining brewing time of a {@link BrewingStand} by a multiplier on {@link BrewEvent}.
 *
 * <p><b>YAML key:</b> {@code modify_brew_time}
 * <p><b>Optional parameters:</b> {@code multiplier} (default 1.0; values &lt; 1 speed up, &gt; 1 slow down)
 */
public final class ModifyBrewTimeMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BrewEvent brewEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;

        if (brewEvent.getContents().getHolder() instanceof BrewingStand stand) {
            int currentTime = stand.getBrewingTime();
            int newTime = (int) Math.round(currentTime * multiplier);
            stand.setBrewingTime(Math.max(1, newTime));
            return true;
        }
        return false;
    }
}
