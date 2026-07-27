package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceExtractEvent;

/**
 * Trigger fired when a player extracts items from a furnace.
 *
 * <p><b>YAML key:</b> {@code furnace_extract}
 */
public record FurnaceExtractTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "furnace_extract"; }

    @Override
    public Class<? extends Event> getEventClass() { return FurnaceExtractEvent.class; }
}
