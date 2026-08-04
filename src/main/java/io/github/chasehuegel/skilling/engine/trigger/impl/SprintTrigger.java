package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Triggers when a player starts sprinting.
 *
 * <p>Dispatch is gated on the sprinting transition to the on-state
 * ({@code PlayerToggleSprintEvent#isSprinting()}), so release does not fire.
 *
 * <p><b>YAML key:</b> {@code sprint}
 */
public record SprintTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sprint"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerToggleSprintEvent.class; }
}
