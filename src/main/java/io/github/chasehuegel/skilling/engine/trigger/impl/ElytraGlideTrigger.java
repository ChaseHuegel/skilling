package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityToggleGlideEvent;

/**
 * Trigger fired when a player starts gliding with an elytra.
 *
 * <p><b>YAML key:</b> {@code elytra_glide}
 */
public record ElytraGlideTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "elytra_glide"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityToggleGlideEvent.class; }
}
