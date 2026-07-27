package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityBreedEvent;

/**
 * Trigger fired when a player breeds animals.
 *
 * <p><b>YAML key:</b> {@code breed_animals}
 */
public record BreedAnimalsTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "breed_animals"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityBreedEvent.class; }
}
