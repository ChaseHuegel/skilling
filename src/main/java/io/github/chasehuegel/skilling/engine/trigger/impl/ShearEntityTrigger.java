package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerShearEntityEvent;

public final class ShearEntityTrigger implements SkillTrigger {
    @Override
    public String getKey() { return "player_shear"; }
    @Override
    public Class<? extends Event> getEventClass() { return PlayerShearEntityEvent.class; }
}
