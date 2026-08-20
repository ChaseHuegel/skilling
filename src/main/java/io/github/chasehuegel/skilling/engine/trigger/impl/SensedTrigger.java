package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockReceiveGameEvent;

/**
 * Trigger fired when a sculpt sensor or shrieker receives a vibration.
 *
 * <p>Maps to the cancellable {@link BlockReceiveGameEvent}, so a bound ability
 * can suppress the sensor silently (e.g. while sneaking).
 *
 * <p><b>YAML key:</b> {@code sensed}
 */
public record SensedTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sensed"; }

    @Override
    public Class<? extends Event> getEventClass() { return BlockReceiveGameEvent.class; }
}
