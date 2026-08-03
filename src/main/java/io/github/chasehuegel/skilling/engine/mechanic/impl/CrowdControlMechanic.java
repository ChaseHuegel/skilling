package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
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
 * {@code radius} (default 5), {@code targets} (default {@code hostiles})
 */
public record CrowdControlMechanic() implements SkillMechanic {
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
        var effect = new PotionEffect(type, duration, amplifier);
        de.getEntity().getNearbyEntities(radius, radius, radius).stream()
            .filter(e -> e instanceof LivingEntity && !e.equals(player))
            .map(e -> (LivingEntity) e)
            .filter(e -> AuraTargetFilter.accepts(targets, e))
            .forEach(e -> e.addPotionEffect(effect));
        return true;
    }
}
