package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.SmithItemEvent;

/**
 * Trigger fired when a player takes an item out of a smithing table.
 *
 * <p><b>YAML key:</b> {@code smith}
 */
public record SmithTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "smith"; }

    @Override
    public Class<? extends Event> getEventClass() { return SmithItemEvent.class; }
}
