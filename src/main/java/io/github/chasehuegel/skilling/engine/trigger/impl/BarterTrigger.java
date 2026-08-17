package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PiglinBarterEvent;

/**
 * Trigger fired when a piglin trades with a player by bartering.
 *
 * <p>This event does not name the player, so the dispatcher routes it to
 * nearby players of the bartering piglin.
 *
 * <p><b>YAML key:</b> {@code barter}
 */
public record BarterTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "barter"; }

    @Override
    public Class<? extends Event> getEventClass() { return PiglinBarterEvent.class; }
}
