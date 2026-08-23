package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Trigger fired when a player milks a cow or goat.
 *
 * <p><b>YAML key:</b> {@code milk_animal}
 */
public record MilkAnimalTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "milk_animal"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEntityEvent.class; }
}