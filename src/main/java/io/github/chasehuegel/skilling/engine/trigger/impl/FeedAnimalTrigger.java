package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Trigger fired when a player feeds a farm animal (a seed or other food item on
 * an animal) to grow or breed it.
 *
 * <p><b>YAML key:</b> {@code feed_animal}
 */
public record FeedAnimalTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "feed_animal"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEntityEvent.class; }
}