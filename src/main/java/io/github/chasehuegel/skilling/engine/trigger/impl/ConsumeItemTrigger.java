package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Trigger fired when a player consumes an item (food, potion, etc.).
 *
 * <p><b>YAML key:</b> {@code consume_item}
 */
public record ConsumeItemTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "consume_item"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerItemConsumeEvent.class; }
}
