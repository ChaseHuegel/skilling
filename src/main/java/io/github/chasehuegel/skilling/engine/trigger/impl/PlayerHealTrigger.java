package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * Trigger fired whenever a player regains health from any source (food, the
 * regeneration effect, an instant-health potion, and event-driven healing from
 * other plugins).
 *
 * <p><b>YAML key:</b> {@code player_heal}
 */
public record PlayerHealTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "player_heal"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityRegainHealthEvent.class; }
}