package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

public record EntityDamageTakenTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "entity_damage_taken"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDamageEvent.class; }
}
