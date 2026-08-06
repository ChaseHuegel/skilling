package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Trigger fired when a player left-clicks a block.
 *
 * <p><b>YAML key:</b> {@code left_click_block}
 */
public record LeftClickBlockTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "left_click_block"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
