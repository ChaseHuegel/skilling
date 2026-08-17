package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player completes a trade with a villager.
 *
 * <p><b>YAML key:</b> {@code trade}
 */
public record TradeTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "trade"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerTradeEvent.class; }
}
