package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Trigger fired when the vehicle the player is riding takes damage.
 *
 * <p><b>YAML key:</b> {@code mount_damage_taken}
 */
public record MountDamageTakenTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "mount_damage_taken"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDamageEvent.class; }
}