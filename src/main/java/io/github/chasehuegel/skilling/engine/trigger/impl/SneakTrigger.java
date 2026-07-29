package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Triggers when a player starts or stops sneaking.
 *
 * <p><b>YAML key:</b> {@code sneak}
 */
public record SneakTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sneak"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerToggleSneakEvent.class; }
}
