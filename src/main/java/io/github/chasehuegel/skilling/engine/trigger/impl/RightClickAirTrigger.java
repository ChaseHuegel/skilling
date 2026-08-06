package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Trigger fired when a player right-clicks air.
 *
 * <p><b>YAML key:</b> {@code right_click_air}
 */
public record RightClickAirTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "right_click_air"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
