package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Trigger fired when a player takes fall damage.
 *
 * <p>The listener dispatches this only for {@link EntityDamageEvent}s whose cause
 * is {@link EntityDamageEvent.DamageCause#FALL}, so a fall that is negated before
 * dispatch (e.g. by feather falling or water) never raises it.
 *
 * <p><b>YAML key:</b> {@code fall_damage}
 */
public record FallDamageTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "fall_damage"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDamageEvent.class; }
}
