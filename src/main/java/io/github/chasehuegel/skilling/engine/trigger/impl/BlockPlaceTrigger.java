package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Trigger fired when a player places a block.
 *
 * <p><b>YAML key:</b> {@code block_place}
 */
public record BlockPlaceTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "block_place"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockPlaceEvent.class; }
}
