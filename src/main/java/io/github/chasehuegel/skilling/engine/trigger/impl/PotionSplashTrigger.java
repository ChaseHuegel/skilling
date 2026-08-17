package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PotionSplashEvent;

/**
 * Trigger fired when a player throws a splash or lingering potion that
 * breaks on impact.
 *
 * <p>The dispatcher covers both {@link PotionSplashEvent} and
 * {@link org.bukkit.event.entity.LingeringPotionSplashEvent}; the declared
 * event class names the first so {@code scaling: damage} validation and the
 * trigger index can key on a single class.
 *
 * <p><b>YAML key:</b> {@code potion_splash}
 */
public record PotionSplashTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "potion_splash"; }

    @Override
    public Class<? extends Event> getEventClass() { return PotionSplashEvent.class; }
}
