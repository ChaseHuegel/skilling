package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;

/**
 * Combined "stealth trip-trap" trigger: fires on either a physical interaction
 * (pressure plates, weighted plates, tripwires) or a sculpt vibration receive.
 *
 * <p>This is a convenience union dispatched by the engine from both the
 * {@link org.bukkit.event.player.PlayerInteractEvent} ({@code Action.PHYSICAL})
 * path and the {@link org.bukkit.event.block.BlockReceiveGameEvent} path, so an
 * ability can cover all silent-travel hazards with one trigger. Its event class
 * is the {@link Event} supertype because it maps to more than one concrete
 * event; that is safe because it is only consulted for damage-scaled XP gating
 * (never a damage source).
 *
 * <p><b>YAML key:</b> {@code trip_trap}
 */
public record TripTrapTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "trip_trap"; }

    @Override
    public Class<? extends Event> getEventClass() { return Event.class; }
}
