package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player ride horse.
 *
 * <p><b>YAML key:</b> {@code ride_horse}
 */
public record RideHorseTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "ride_horse"; }

    @Override
    public Class<? extends Event> getEventClass() { return org.bukkit.event.player.PlayerInteractEntityEvent.class; }
}
