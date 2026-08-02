package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BrewingStartEvent;

/**
 * Trigger fired when a brewing stand begins a new brewing cycle.
 *
 * <p><b>YAML key:</b> {@code brew_start}
 */
public record BrewStartTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "brew_start"; }

    @Override
    public Class<? extends Event> getEventClass() { return BrewingStartEvent.class; }
}
