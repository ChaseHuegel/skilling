package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public record FurnaceExtractTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "furnace_extract"; }

    @Override
    public Class<? extends Event> getEventClass() { return FurnaceExtractEvent.class; }
}
