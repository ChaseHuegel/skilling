package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.block.CompostItemEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when an item is composted into a composter block.
 *
 * <p>The dispatcher routes this to nearby players of the composter block,
 * because a {@link CompostItemEvent} does not carry the feeding player.
 *
 * <p><b>YAML key:</b> {@code compost}
 */
public record CompostTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "compost"; }

    @Override
    public Class<? extends Event> getEventClass() { return CompostItemEvent.class; }
}
