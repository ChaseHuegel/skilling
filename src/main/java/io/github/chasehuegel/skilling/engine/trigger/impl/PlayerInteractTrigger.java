package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

public record PlayerInteractTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "player_interact"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
