package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * Applies a directional velocity impulse (knockback) to the damaged entity or
 * all living entities in a radius.
 *
 * <p>Both paths gate targets through {@link AuraTargetFilter} and always exclude
 * the caster and any other player, so an ability can never hurl a player into
 * the void or across the map. The {@code radius}, {@code force}, and
 * {@code vertical} params are clamped at execution time so level-scaled
 * evaluator outputs stay bounded too.
 *
 * <p>YAML key: {@code core:knockback}
 * <br>Params:
 * <ul>
 *   <li>{@code force} (double) — horizontal impulse strength, must be &gt; 0 to
 *       act (clamped to [0, 3])</li>
 *   <li>{@code radius} (double, optional, default 0) — if &gt; 0, shoves all living
 *       entities near the player instead of the single damaged entity
 *       (clamped to [0, 32])</li>
 *   <li>{@code vertical} (double, optional, default 0.3) — upward component added
 *       to the impulse (clamped to [0, 1.5])</li>
 *   <li>{@code targets} (string, optional, default {@code hostiles}) — which
 *       living entities receive the knockback; other players are never knocked</li>
 * </ul>
 */
public final class KnockbackMechanic implements SkillMechanic {

    /** Matches Bukkit's entity-search radius cap, mirroring {@link AllyAuraMechanic}. */
    static final double MAX_RADIUS = 32.0;

    /** Sane impulse cap so a level-scaled force cannot launch entities across the map. */
    static final double MAX_FORCE = 3.0;

    /** Sane upward cap so a level-scaled vertical cannot rocket entities skyward. */
    static final double MAX_VERTICAL = 1.5;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double force = clampForce(((Number) params.getOrDefault("force", 0.0)).doubleValue());
        if (force <= 0) return false;
        double radius = clampRadius(((Number) params.getOrDefault("radius", 0.0)).doubleValue());
        double vertical = clampVertical(((Number) params.getOrDefault("vertical", 0.3)).doubleValue());
        String targets = String.valueOf(params.getOrDefault("targets", "hostiles"));

        Vector impulse = player.getLocation().getDirection().multiply(force);
        impulse.setY(impulse.getY() + vertical);

        if (radius > 0) {
            for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
                if (accepts(targets, target, player)) {
                    target.setVelocity(impulse);
                }
            }
            return true;
        }

        if (event instanceof EntityDamageByEntityEvent de
                && de.getEntity() instanceof LivingEntity target
                && accepts(targets, target, player)) {
            target.setVelocity(impulse);
            return true;
        }

        return false;
    }

    /**
     * Whether a target receives the knockback: never the caster, never another
     * player (PvP protection), and otherwise only entities accepted by the
     * {@code targets} filter.
     */
    private static boolean accepts(String targets, LivingEntity target, Player caster) {
        if (target.equals(caster) || target instanceof Player) return false;
        return AuraTargetFilter.accepts(targets, target);
    }

    static double clampRadius(double radius) {
        return Math.max(0.0, Math.min(radius, MAX_RADIUS));
    }

    static double clampForce(double force) {
        return Math.max(0.0, Math.min(force, MAX_FORCE));
    }

    static double clampVertical(double vertical) {
        return Math.max(0.0, Math.min(vertical, MAX_VERTICAL));
    }
}
