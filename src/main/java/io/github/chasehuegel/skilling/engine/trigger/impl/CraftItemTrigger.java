package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * Trigger fired when a player crafts an item.
 *
 * <p><b>YAML key:</b> {@code craft_item}
 */
public record CraftItemTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "craft_item"; }

    @Override
    public Class<? extends Event> getEventClass() { return CraftItemEvent.class; }
}
