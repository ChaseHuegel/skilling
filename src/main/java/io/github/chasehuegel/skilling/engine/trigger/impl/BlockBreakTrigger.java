package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Trigger fired when a player breaks a block.
 *
 * <p><b>YAML key:</b> {@code block_break}
 */
public record BlockBreakTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "block_break"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockBreakEvent.class; }
}
