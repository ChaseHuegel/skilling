package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Trigger fired when a projectile hits a block or entity.
 *
 * <p>Used by mechanics that act on impact (e.g. {@code core:projectile_return}),
 * as opposed to {@code launch_projectile} which fires when the projectile is
 * thrown.
 *
 * <p><b>YAML key:</b> {@code projectile_hit}
 */
public record ProjectileHitTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "projectile_hit"; }

    @Override
    public Class<? extends Event> getEventClass() { return ProjectileHitEvent.class; }
}
