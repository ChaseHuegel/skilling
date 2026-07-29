package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player enchant item.
 *
 * <p><b>YAML key:</b> {@code enchant_item}
 */
public record EnchantItemTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "enchant_item"; }

    @Override
    public Class<? extends Event> getEventClass() { return org.bukkit.event.enchantment.EnchantItemEvent.class; }
}
