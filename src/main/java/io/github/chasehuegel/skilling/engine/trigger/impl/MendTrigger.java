package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemMendEvent;

/**
 * Trigger fired when an item with the Mending enchantment repairs itself
 * from experience orbs.
 *
 * <p><b>YAML key:</b> {@code mend}
 */
public record MendTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "mend"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerItemMendEvent.class; }
}
