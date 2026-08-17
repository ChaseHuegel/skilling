package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.entity.EntityFertilizeEggEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player breeds a sniffer by feeding it seeds.
 *
 * <p>The dispatcher attributes the event to the breeding player via
 * {@link EntityFertilizeEggEvent#getBreeder()}, which is null when the egg
 * was fertilized without a player.
 *
 * <p><b>YAML key:</b> {@code sniffer}
 */
public record SnifferTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sniffer"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityFertilizeEggEvent.class; }
}
