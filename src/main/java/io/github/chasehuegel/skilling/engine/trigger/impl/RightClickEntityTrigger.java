package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Trigger fired when a player right-clicks an entity.
 *
 * <p><b>YAML key:</b> {@code right_click_entity}
 */
public record RightClickEntityTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "right_click_entity"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEntityEvent.class; }
}
