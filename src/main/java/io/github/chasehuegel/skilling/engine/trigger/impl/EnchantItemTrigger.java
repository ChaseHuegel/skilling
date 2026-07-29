package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;

/**
 * Triggers when a player enchants an item at an enchanting table.
 *
 * <p><b>YAML key:</b> {@code enchant_item}
 */
public record EnchantItemTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "enchant_item"; }

    @Override
    public Class<? extends Event> getEventClass() { return EnchantItemEvent.class; }
}
