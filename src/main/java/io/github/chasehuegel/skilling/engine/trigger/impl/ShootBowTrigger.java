package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;

/**
 * Trigger fired when a player shoots a bow or crossbow.
 *
 * <p><b>YAML key:</b> {@code shoot_bow}
 */
public record ShootBowTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "shoot_bow"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityShootBowEvent.class; }
}
