package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Trigger fired when a player dies.
 *
 * <p><b>YAML key:</b> {@code player_death}
 */
public record PlayerDeathTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "player_death"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerDeathEvent.class; }
}
