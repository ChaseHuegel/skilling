package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies an AoE status effect to nearby entities when damaging a target.
 *
 * <p>This is the offensive counterpart to the buff auras: it defaults to
 * {@code targets: hostiles} so the debuff only lands on monsters and angered
 * neutrals — never on the caster's allies. Set {@code targets} to {@code allies}
 * or {@code all} to override; unknown values fall back to {@code allies} (the
 * safe default shared with the buff auras).
 *
 * <p>YAML key: {@code core:crowd_control}
 * <br>Params: {@code effect} (namespaced key or legacy numeric ID),
 * {@code duration} (default 3), {@code amplifier} (default 0),
 * {@code radius} (default 5, clamped to [0, 32]),
 * {@code targets} (default {@code hostiles})
 */
public record CrowdControlMechanic() implements SkillMechanic {

    /** Matches Bukkit's entity-search radius cap, mirroring {@link AllyAuraMechanic}. */
    static final double MAX_RADIUS = 32.0;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!player.equals(EntityDamageResolver.resolveDamagerPlayer(de))) return false;
        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"));
        if (type == null) return false;
        int duration = ((Number) params.getOrDefault("duration", 3)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0)).intValue();
        double radius = ((Number) params.getOrDefault("radius", 5.0)).doubleValue();
        String targets = String.valueOf(params.getOrDefault("targets", "hostiles"));
        return apply(de.getEntity(), player, new PotionEffect(type, duration, amplifier), radius, targets);
    }

    /**
     * Applies the effect to every accepted nearby living entity except the caster,
     * centering the scan on the damaged entity.
     *
     * @param origin  the entity at the center of the scan (the damaged entity)
     * @param caster  the casting player, always excluded
     * @param effect  the resolved effect to apply
     * @param radius  the search radius in blocks (clamped to [0, 32])
     * @param targets the {@code allies | hostiles | all} filter
     * @return true
     */
    static boolean apply(Entity origin, Player caster, PotionEffect effect, double radius, String targets) {
        double clamped = clampRadius(radius);
        origin.getNearbyEntities(clamped, clamped, clamped).stream()
            .filter(e -> e instanceof LivingEntity && !e.equals(caster))
            .map(e -> (LivingEntity) e)
            .filter(e -> AuraTargetFilter.accepts(targets, e))
            .forEach(e -> e.addPotionEffect(effect));
        return true;
    }

    /**
     * Clamps a radius to Bukkit's [0, 32] entity-search bounds so an oversized
     * config can never trigger an unbounded nearby-entity scan.
     *
     * @param radius the requested radius
     * @return the clamped radius
     */
    static double clampRadius(double radius) {
        return Math.max(0.0, Math.min(radius, MAX_RADIUS));
    }
}
