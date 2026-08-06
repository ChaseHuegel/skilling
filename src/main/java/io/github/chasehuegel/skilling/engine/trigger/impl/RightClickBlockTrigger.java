package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Trigger fired when a player right-clicks a block.
 *
 * <p><b>YAML key:</b> {@code right_click_block}
 */
public record RightClickBlockTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "right_click_block"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
