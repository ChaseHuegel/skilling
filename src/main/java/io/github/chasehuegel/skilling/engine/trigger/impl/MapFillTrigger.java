package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.player.PlayerMapFilledEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player's map fills with terrain for the first time.
 *
 * <p><b>YAML key:</b> {@code map_fill}
 */
public record MapFillTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "map_fill"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerMapFilledEvent.class; }
}
