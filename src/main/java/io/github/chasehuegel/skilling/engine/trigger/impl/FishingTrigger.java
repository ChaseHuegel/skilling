package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Trigger fired when a player catches a fish.
 *
 * <p>Dispatch is gated on the {@code CAUGHT_FISH} state only: casts, bites,
 * reels, and failed attempts do not fire, so a single cast-and-catch grants
 * exactly one activation.
 *
 * <p><b>YAML key:</b> {@code fishing}
 */
public record FishingTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "fishing"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerFishEvent.class; }
}
