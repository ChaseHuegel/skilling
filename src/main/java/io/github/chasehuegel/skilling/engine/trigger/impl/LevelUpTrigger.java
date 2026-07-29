package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player levels up a Skilling skill.
 *
 * <p><b>YAML key:</b> {@code level_up}
 */
public record LevelUpTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "level_up"; }

    @Override
    public Class<? extends Event> getEventClass() { return io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent.class; }
}
