package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Trigger fired when a player physically steps onto or into a block.
 *
 * <p>Maps to {@link PlayerInteractEvent} with {@code Action.PHYSICAL}, which
 * covers pressure plates, weighted plates, and tripwires.
 *
 * <p><b>YAML key:</b> {@code physical_interaction}
 */
public record PhysicalInteractionTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "physical_interaction"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInteractEvent.class; }
}
