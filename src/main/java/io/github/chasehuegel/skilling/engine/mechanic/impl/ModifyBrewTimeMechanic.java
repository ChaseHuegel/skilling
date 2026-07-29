package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BrewingStartEvent;
import java.util.Map;

/**
 * Modifies the brewing time when a new batch starts by a multiplier.
 * Hooks {@link BrewingStartEvent} (Paper API), which fires when a brewing stand begins a new
 * brewing cycle — before the cycle starts, so the time multiplier affects the current batch
 * rather than the next one.
 *
 * <p><b>YAML key:</b> {@code modify_brew_time}
 * <p><b>Optional parameters:</b> {@code multiplier} (default 1.0; values &lt; 1 speed up, &gt; 1 slow down)
 */
public final class ModifyBrewTimeMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BrewingStartEvent brewEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;

        int totalTime = brewEvent.getBrewingTime();
        int newTime = (int) Math.round(totalTime * multiplier);
        brewEvent.setBrewingTime(Math.max(1, newTime));
        return true;
    }
}
