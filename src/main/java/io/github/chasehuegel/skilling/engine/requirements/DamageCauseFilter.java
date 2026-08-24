package io.github.chasehuegel.skilling.engine.requirements;

import java.util.Map;
import java.util.Set;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Resolves the {@code cause} state-filter keyword ({@code burn}, {@code fire},
 * {@code lava}, {@code drowning}, {@code suffocation}, {@code freeze},
 * {@code lightning}, {@code cactus}, {@code starvation},
 * {@code fly_into_wall}, or the compound {@code environmental} hazard set)
 * against the current {@link EntityDamageEvent} cause.
 *
 * <p>The filter fails closed: any event that is not an {@link EntityDamageEvent}
 * (or carries another cause) returns {@code false}, so a {@code cause} reference
 * is only meaningful on the {@code entity_damage_taken} trigger. The valid-keyword
 * set shared with load-time validation lives here so the parser and the event path
 * cannot drift apart.
 */
public final class DamageCauseFilter {

    /** Keyword to the damage causes it matches. Immutable, not author-facing (design decision, ISSUE-301). */
    private static final Map<String, Set<DamageCause>> KEYWORDS = Map.ofEntries(
            Map.entry("fire", Set.of(DamageCause.FIRE)),
            Map.entry("lava", Set.of(DamageCause.LAVA)),
            // burn covers the ongoing fire-tick hazard. Lava is deliberately kept
            // out of burn and gated behind its own keyword so the rarer, heavier
            // lava damage can price a distinct hazard tier.
            Map.entry("burn", Set.of(DamageCause.FIRE, DamageCause.FIRE_TICK)),
            Map.entry("drowning", Set.of(DamageCause.DROWNING)),
            Map.entry("suffocation", Set.of(DamageCause.SUFFOCATION)),
            Map.entry("freeze", Set.of(DamageCause.FREEZE)),
            Map.entry("lightning", Set.of(DamageCause.LIGHTNING)),
            Map.entry("cactus", Set.of(DamageCause.CONTACT)),
            Map.entry("starvation", Set.of(DamageCause.STARVATION)),
            Map.entry("fly_into_wall", Set.of(DamageCause.FLY_INTO_WALL)),
            // The full survivable hazard set: every environment the heavy-armor
            // skill's L75 "Walking Fortress" blunts. Broad by design so one
            // filter reads as "walk through anything".
            Map.entry("environmental", Set.of(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA,
                    DamageCause.DROWNING, DamageCause.SUFFOCATION, DamageCause.CONTACT,
                    DamageCause.STARVATION, DamageCause.ENTITY_EXPLOSION, DamageCause.BLOCK_EXPLOSION,
                    DamageCause.FLY_INTO_WALL))
    );

    private DamageCauseFilter() {}

    /**
     * Whether the event's damage cause matches the keyword.
     *
     * @param event   the triggering event
     * @param keyword the filter value (e.g. {@code burn})
     * @return true when the event is a damage event whose cause matches the keyword
     */
    public static boolean evaluate(Event event, String keyword) {
        Set<DamageCause> causes = KEYWORDS.get(keyword == null ? "" : keyword.toLowerCase());
        if (causes == null) return false;
        if (!(event instanceof EntityDamageEvent de)) return false;
        return causes.contains(de.getCause());
    }

    /**
     * Whether the value is a recognized cause keyword (load-time validation).
     *
     * @param keyword the raw {@code cause} value
     * @return true if the value names a supported keyword
     */
    public static boolean isValidValue(String keyword) {
        return keyword != null && KEYWORDS.containsKey(keyword.toLowerCase());
    }
}