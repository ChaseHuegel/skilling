package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.BrewEvent;

public record BrewPotionTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "brew_potion"; }

    @Override
    public Class<? extends Event> getEventClass() { return BrewEvent.class; }
}
