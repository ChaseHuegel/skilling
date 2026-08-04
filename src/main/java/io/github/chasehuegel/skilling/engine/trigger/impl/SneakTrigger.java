package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Triggers when a player starts sneaking.
 *
 * <p>Dispatch is gated on the sneaking transition to the on-state
 * ({@code PlayerToggleSneakEvent#isSneaking()}), so release does not fire.
 *
 * <p><b>YAML key:</b> {@code sneak}
 */
public record SneakTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sneak"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerToggleSneakEvent.class; }
}
