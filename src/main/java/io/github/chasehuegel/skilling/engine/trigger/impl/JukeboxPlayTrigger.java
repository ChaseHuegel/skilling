package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Trigger fired when a player inserts a music disc into an empty jukebox.
 *
 * <p>The dispatcher routes a right-click {@code PlayerInteractEvent} on a jukebox
 * block only when the jukebox is actually holding a disc afterwards, so an
 * ejection (clicking an occupied jukebox) or a click with no disc never fires.
 *
 * <p><b>YAML key:</b> {@code jukebox_play}
 */
public record JukeboxPlayTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "jukebox_play"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
