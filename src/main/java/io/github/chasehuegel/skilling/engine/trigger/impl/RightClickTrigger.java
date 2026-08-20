package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Trigger fired when a player right-clicks with the main hand, on any surface.
 *
 * <p>Unlike {@code right_click_air} and {@code right_click_block}, this is the
 * union of both — a right-click that targets a block is classified as
 * {@code RIGHT_CLICK_BLOCK}, while one that targets nothing for the block
 * interaction range is {@code RIGHT_CLICK_AIR}. A single trigger that accepts
 * either is the reliable way to capture a "right-click use" (e.g. playing a
 * goat horn) regardless of what the cursor happens to hit. Left-clicks never
 * fire it. The dispatcher also routes {@code PlayerInteractEntityEvent} into
 * this trigger so right-clicking an entity counts too.
 *
 * <p><b>YAML key:</b> {@code right_click}
 */
public record RightClickTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "right_click"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
