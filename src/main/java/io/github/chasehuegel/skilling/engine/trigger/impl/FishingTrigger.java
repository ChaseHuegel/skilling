package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Trigger fired when a player casts or reels in a fishing line.
 *
 * <p><b>YAML key:</b> {@code fishing}
 */
public record FishingTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "fishing"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerFishEvent.class; }
}
