package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityResurrectEvent;

/**
 * Trigger fired when a player is resurrected by a Totem of Undying.
 *
 * <p><b>YAML key:</b> {@code resurrect}
 */
public record ResurrectTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "resurrect"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityResurrectEvent.class; }
}
