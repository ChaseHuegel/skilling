package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

public final class FieldAuraMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"));
        if (type == null) return false;
        double radius = ((Number) params.getOrDefault("radius", 8.0)).doubleValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        var effect = new PotionEffect(type, duration, amplifier);
        player.addPotionEffect(effect);
        for (LivingEntity target : player.getLocation().getNearbyLivingEntities(radius)) {
            if (target instanceof Player || target.getUniqueId().equals(player.getUniqueId())) continue;
            target.addPotionEffect(effect);
        }
        return true;
    }
}
