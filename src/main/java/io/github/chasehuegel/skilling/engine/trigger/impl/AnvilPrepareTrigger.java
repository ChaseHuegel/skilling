package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.PrepareAnvilEvent;

/**
 * Trigger fired on every anvil inventory prepare, whether or not the inputs
 * resolve to a real repair.
 *
 * <p>Unlike the gated {@code repair} trigger (which only fires when a materialized
 * result is present), this fires even when the anvil would show "Too Expensive"
 * (a null result). It lets an economy passive such as {@code core:uncap_repair}
 * raise the anvil's maximum repair cost before the player commits a repair.
 *
 * <p><b>YAML key:</b> {@code anvil_prepare}
 */
public record AnvilPrepareTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "anvil_prepare"; }

    @Override
    public Class<? extends Event> getEventClass() { return PrepareAnvilEvent.class; }
}