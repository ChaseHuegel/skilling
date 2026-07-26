package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

public record BlockBreakTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "block_break"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockBreakEvent.class; }
}
