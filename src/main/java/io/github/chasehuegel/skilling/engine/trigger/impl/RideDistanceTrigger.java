package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/**
 * Trigger fired while a player is riding a moving vehicle.
 *
 * <p>The listener throttles this dispatch so it grants a steady, time-gated
 * trickle rather than one grant per block moved.
 *
 * <p><b>YAML key:</b> {@code ride_distance}
 */
public record RideDistanceTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "ride_distance"; }

    @Override
    public Class<? extends Event> getEventClass() { return VehicleMoveEvent.class; }
}