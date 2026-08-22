package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Trigger fired the moment a bobber is cast into the water.
 *
 * <p>Dispatch is gated on the {@code FISHING} state only, so the cast event fires
 * exactly once per throw and before any bite. This is the early hook used by the
 * {@code core:fishing_speed} mechanic to shorten the pending cast-to-bite wait:
 * acting at the cast lets the shortened wait take effect for the whole pending
 * catch. Casts, bites, reels, and caught fish fire nothing.
 *
 * <p><b>YAML key:</b> {@code fishing_cast}
 */
public record FishingCastTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "fishing_cast"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerFishEvent.class; }
}