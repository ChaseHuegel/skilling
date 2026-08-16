package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player has slept long enough to count as passing the
 * night or storm.
 *
 * <p>Backed by {@link PlayerDeepSleepEvent}, so checking into a bed and getting
 * back out without sleeping does not raise it.
 *
 * <p><b>YAML key:</b> {@code sleep}
 */
public record SleepTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sleep"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerDeepSleepEvent.class; }
}