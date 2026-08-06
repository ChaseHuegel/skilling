package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Trigger fired when a player attacks an entity directly with a left-click.
 *
 * <p>Bukkit models a left-click on an entity as an attack
 * ({@link EntityDamageByEntityEvent}). Projectile attacks are not left-clicks and
 * are served by the {@code entity_damage} and {@code shoot_bow} triggers instead.
 *
 * <p><b>YAML key:</b> {@code left_click_entity}
 */
public record LeftClickEntityTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "left_click_entity"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityDamageByEntityEvent.class; }
}
