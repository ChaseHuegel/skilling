package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player sneak.
 *
 * <p><b>YAML key:</b> {@code sneak}
 */
public record SneakTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sneak"; }

    @Override
    public Class<? extends Event> getEventClass() { return org.bukkit.event.player.PlayerToggleSneakEvent.class; }
}
