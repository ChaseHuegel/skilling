package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockGrowEvent;

public record CropGrowTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "crop_grow"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockGrowEvent.class; }
}
