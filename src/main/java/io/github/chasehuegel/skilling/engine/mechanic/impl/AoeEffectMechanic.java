package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

/**
 * Applies a potion effect to all living entities within a radius of the player, excluding the player themselves.
 *
 * <p><b>YAML key:</b> {@code aoe_effect}
 * <p><b>Required parameters:</b> {@code effect} (potion effect type name)
 * <p><b>Optional parameters:</b> {@code radius} (default 5.0), {@code duration} (default 5s), {@code amplifier} (default 0)
 */
public final class AoeEffectMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        String effectName = (String) params.getOrDefault("effect", "");
        if (effectName.isBlank()) return false;
        PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
        if (type == null) return false;

        double radius = ((Number) params.getOrDefault("radius", 5.0)).doubleValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();

        for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
            if (!target.equals(player)) {
                target.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
        return true;
    }
}
