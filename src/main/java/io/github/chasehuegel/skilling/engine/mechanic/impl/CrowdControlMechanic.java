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
 * <p>YAML key: {@code core:crowd_control}
 * <br>Params: {@code effect}, {@code duration} (default 3), {@code amplifier} (default 0), {@code radius} (default 5)
 */
public record CrowdControlMechanic() implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!de.getDamager().equals(player)) return false;
        String effectName = (String) params.get("effect");
        if (effectName == null || effectName.isBlank()) return false;
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type == null) return false;
        int duration = ((Number) params.getOrDefault("duration", 3)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0)).intValue();
        double radius = ((Number) params.getOrDefault("radius", 5.0)).doubleValue();
        var effect = new PotionEffect(type, duration, amplifier);
        de.getEntity().getNearbyEntities(radius, radius, radius).stream()
            .filter(e -> e instanceof LivingEntity && !e.equals(player))
            .map(e -> (LivingEntity) e)
            .forEach(e -> e.addPotionEffect(effect));
        return true;
    }
}
