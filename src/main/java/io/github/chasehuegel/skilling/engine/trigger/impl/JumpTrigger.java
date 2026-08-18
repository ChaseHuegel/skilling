package io.github.chasehuegel.skilling.engine.trigger.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Triggers when a player jumps.
 *
 * <p>Paper fires {@code PlayerJumpEvent} when the server detects the player
 * jumping, so a jump is a real, costly input rather than a toggle state.
 *
 * <p><b>YAML key:</b> {@code jump}
 */
public record JumpTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "jump"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerJumpEvent.class; }
}