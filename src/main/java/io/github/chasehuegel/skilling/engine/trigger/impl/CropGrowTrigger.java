package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockGrowEvent;

/**
 * Trigger fired when a crop or plant naturally grows.
 *
 * <p><b>YAML key:</b> {@code crop_grow}
 */
public record CropGrowTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "crop_grow"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockGrowEvent.class; }
}
