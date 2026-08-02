package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.PrepareAnvilEvent;

/**
 * Trigger fired when a player opens an anvil or changes its inputs.
 *
 * <p><b>YAML key:</b> {@code repair}
 */
public record RepairTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "repair"; }

    @Override
    public Class<? extends Event> getEventClass() { return PrepareAnvilEvent.class; }
}
