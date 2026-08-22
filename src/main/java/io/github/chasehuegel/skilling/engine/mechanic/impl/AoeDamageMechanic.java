package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Deals a scaled share of the triggering melee hit to every hostile in a radius
 * around the damaged entity, so a single blow cleaves through a pack.
 *
 * <p>The main target already receives the event's normal damage through vanilla
 * (and any {@code core:modify_damage} scalar), so this mechanic strikes the
 * <em>other</em> living entities in {@code radius} — never the primary victim,
 * the caster, or players. Each adjacent foe takes {@code multiplier} times the
 * event's raw damage through the normal pipeline.
 *
 * <p>YAML key: {@code core:aoe_damage}
 * <br>Params: {@code radius} (default 3, clamped to [0, 32]),
 * {@code multiplier} (default 1.0, the fraction of the hit dealt to each
 * adjacent foe), {@code targets} (default {@code hostiles}, resolved by
 * {@link AuraTargetFilter}: {@code allies | hostiles | all}).
 */
public record AoeDamageMechanic() implements SkillMechanic {

    /** Matches Bukkit's entity-search radius cap, mirroring {@link CrowdControlMechanic}. */
    static final double MAX_RADIUS = 32.0;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!player.equals(EntityDamageResolver.resolveDamagerPlayer(de))) return false;
        Entity source = de.getEntity();
        if (!(source instanceof LivingEntity origin)) return false;
        double radius = ((Number) params.getOrDefault("radius", 3.0)).doubleValue();
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (radius <= 0 || multiplier <= 0) return false;
        String targets = String.valueOf(params.getOrDefault("targets", "hostiles"));
        double amount = Math.max(0.0, de.getDamage() * multiplier);
        if (amount <= 0) return false;

        double clamped = Math.max(0.0, Math.min(radius, MAX_RADIUS));
        boolean hitAnyone = false;
        for (Entity candidate : origin.getNearbyEntities(clamped, clamped, clamped)) {
            if (!(candidate instanceof LivingEntity living)) continue;
            if (living.equals(player) || living.equals(origin)) continue;
            if (!AuraTargetFilter.accepts(targets, living)) continue;
            living.damage(amount, player);
            hitAnyone = true;
        }
        return hitAnyone;
    }
}