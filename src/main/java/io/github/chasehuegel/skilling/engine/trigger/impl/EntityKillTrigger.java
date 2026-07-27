package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Trigger fired when a player kills an entity.
 *
 * <p><b>YAML key:</b> {@code entity_kill}
 */
public record EntityKillTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "entity_kill"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDeathEvent.class; }
}
