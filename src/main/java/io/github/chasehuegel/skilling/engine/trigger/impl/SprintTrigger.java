package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player sprint.
 *
 * <p><b>YAML key:</b> {@code sprint}
 */
public record SprintTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sprint"; }

    @Override
    public Class<? extends Event> getEventClass() { return org.bukkit.event.player.PlayerToggleSprintEvent.class; }
}
