package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

public final class ApplyStatusMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
        if (!(damageEvent.getEntity() instanceof LivingEntity target)) return false;

        String effectName = (String) params.getOrDefault("effect", "");
        if (effectName.isBlank()) return false;
        PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
        if (type == null) return false;

        int duration = ((Number) params.getOrDefault("duration", 3.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        target.addPotionEffect(new PotionEffect(type, duration, amplifier));
        return true;
    }
}
