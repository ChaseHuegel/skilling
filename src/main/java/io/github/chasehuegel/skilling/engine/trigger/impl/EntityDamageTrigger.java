package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public record EntityDamageTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "entity_damage"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDamageByEntityEvent.class; }
}
