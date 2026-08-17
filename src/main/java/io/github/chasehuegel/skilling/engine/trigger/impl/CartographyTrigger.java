package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.player.CartographyItemEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player takes an item out of a cartography table.
 *
 * <p><b>YAML key:</b> {@code cartography}
 */
public record CartographyTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "cartography"; }

    @Override
    public Class<? extends Event> getEventClass() { return CartographyItemEvent.class; }
}
