package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Trigger fired when a fishing bobber hooks a living entity (a mob, not a fish).
 *
 * <p>Dispatch is gated on the {@code CAUGHT_ENTITY} state only, so a rod that
 * hooks a mob on the water's surface fires exactly once per hook, distinct from
 * the {@code fishing} trigger which fires only on {@code CAUGHT_FISH}. Casts,
 * bites, reels, and failed attempts fire nothing.
 *
 * <p><b>YAML key:</b> {@code fishing_hook}
 */
public record FishingHookTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "fishing_hook"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerFishEvent.class; }
}