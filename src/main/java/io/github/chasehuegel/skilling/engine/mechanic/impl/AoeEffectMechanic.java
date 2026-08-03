package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

/**
 * Applies a potion effect to living entities within a radius of the player,
 * excluding the player themselves.
 *
 * <p>By default ({@code targets: allies}) only non-hostile targets receive the
 * effect, so an area heal never heals hostile mobs. Set {@code targets} to
 * {@code hostiles} (only monsters and angered neutrals) or {@code all} (every
 * nearby living entity) to override; unknown values fall back to {@code allies}.
 *
 * <p><b>YAML key:</b> {@code core:aoe_effect}
 * <br>Params: {@code effect} (namespaced key or legacy numeric ID),
 * {@code radius} (default 5.0), {@code duration} (default 5s),
 * {@code amplifier} (default 0), {@code targets} (default {@code allies})
 */
public final class AoeEffectMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"));
        if (type == null) return false;

        double radius = ((Number) params.getOrDefault("radius", 5.0)).doubleValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        String targets = String.valueOf(params.getOrDefault("targets", "allies"));
        return apply(player, new PotionEffect(type, duration, amplifier), radius, targets);
    }

    /**
     * Applies the effect to every accepted nearby living entity except the player.
     *
     * @param player  the casting player
     * @param effect  the resolved effect to apply
     * @param radius  the search radius in blocks
     * @param targets the {@code allies | hostiles | all} filter
     * @return true
     */
    static boolean apply(Player player, PotionEffect effect, double radius, String targets) {
        for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
            if (target.equals(player)) continue;
            if (AuraTargetFilter.accepts(targets, target)) {
                target.addPotionEffect(effect);
            }
        }
        return true;
    }
}
