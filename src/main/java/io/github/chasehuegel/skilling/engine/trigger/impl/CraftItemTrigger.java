package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;

public record CraftItemTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "craft_item"; }

    @Override
    public Class<? extends Event> getEventClass() { return CraftItemEvent.class; }
}
