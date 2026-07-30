package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTameEvent;

public final class TameEntityTrigger implements SkillTrigger {
    @Override
    public String getKey() { return "player_tame"; }
    @Override
    public Class<? extends Event> getEventClass() { return EntityTameEvent.class; }
}
