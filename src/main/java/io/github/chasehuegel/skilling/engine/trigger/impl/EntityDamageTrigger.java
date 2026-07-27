package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Trigger fired when a player damages an entity.
 *
 * <p><b>YAML key:</b> {@code entity_damage}
 */
public record EntityDamageTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "entity_damage"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDamageByEntityEvent.class; }
}
