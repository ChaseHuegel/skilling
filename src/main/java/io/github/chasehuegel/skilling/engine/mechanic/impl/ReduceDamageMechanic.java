package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Map;

/**
 * Reduces the activating player's incoming {@link EntityDamageEvent} damage by a
 * flat percentage.
 *
 * <p>The player must be the damaged entity; otherwise the mechanic is a no-op.
 * Damage is multiplied by {@code (1 - reduction / 100)} so a reduction of {@code
 * 25} softens every qualifying blow to three-quarters of its value. The reduction
 * is deterministic — there is no roll — so a hit that lands is always blunted and
 * the ability reads as internalized armor, not a chance to ignore damage (that is
 * {@code core:cancel_damage}). Source gating is delegated to the ability's
 * {@code cause} state filter (e.g. {@code cause:environmental}).
 *
 * <p><b>YAML key:</b> {@code core:reduce_damage}
 * <br>Params: {@code reduction} (0-100, percentage of incoming damage removed)
 */
public final class ReduceDamageMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double reduction = ((Number) params.getOrDefault("reduction", 0)).doubleValue();
        if (reduction <= 0) return false;
        double reduced = de.getDamage() * (1 - Math.min(100, reduction) / 100);
        de.setDamage(Math.max(0, reduced));
        return true;
    }
}