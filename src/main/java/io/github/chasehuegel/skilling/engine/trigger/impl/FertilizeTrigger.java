package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockFertilizeEvent;

/**
 * Trigger fired when a player uses bonemeal on a block.
 *
 * <p>The dispatcher routes this to {@link BlockFertilizeEvent#getPlayer()} when
 * the player is present, so farming abilities bind to the bonemeal use itself
 * rather than the click that preceded it.
 *
 * <p><b>YAML key:</b> {@code fertilize}
 */
public record FertilizeTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "fertilize"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockFertilizeEvent.class; }
}
