package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * Applies a directional velocity impulse (knockback) to the damaged entity or all living
 * entities in a radius.
 *
 * <p>YAML key: {@code core:knockback}
 * <br>Params:
 * <ul>
 *   <li>{@code force} (double) — horizontal impulse strength, must be &gt; 0 to act</li>
 *   <li>{@code radius} (double, optional, default 0) — if &gt; 0, shoves all living entities
 *       near the player instead of the single damaged entity</li>
 *   <li>{@code vertical} (double, optional, default 0.3) — upward component added to the impulse</li>
 * </ul>
 */
public final class KnockbackMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double force = ((Number) params.getOrDefault("force", 0.0)).doubleValue();
        if (force <= 0) return false;
        double radius = ((Number) params.getOrDefault("radius", 0.0)).doubleValue();
        double vertical = ((Number) params.getOrDefault("vertical", 0.3)).doubleValue();

        Vector impulse = player.getLocation().getDirection().multiply(force);
        impulse.setY(impulse.getY() + vertical);

        if (radius > 0) {
            for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
                if (!target.equals(player)) {
                    target.setVelocity(impulse);
                }
            }
            return true;
        }

        if (event instanceof EntityDamageByEntityEvent de
                && de.getEntity() instanceof LivingEntity target) {
            target.setVelocity(impulse);
            return true;
        }

        return false;
    }
}
