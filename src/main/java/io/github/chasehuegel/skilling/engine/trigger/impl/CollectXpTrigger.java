package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player collect xp.
 *
 * <p><b>YAML key:</b> {@code collect_xp}
 */
public record CollectXpTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "collect_xp"; }

    @Override
    public Class<? extends Event> getEventClass() { return org.bukkit.event.player.PlayerExpChangeEvent.class; }
}
