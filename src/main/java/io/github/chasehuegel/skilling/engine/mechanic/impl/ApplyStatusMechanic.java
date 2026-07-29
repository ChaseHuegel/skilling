package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

/**
 * Applies a potion effect to the entity damaged by the player on {@link EntityDamageByEntityEvent}.
 *
 * <p><b>YAML key:</b> {@code apply_status}
 * <p><b>Required parameters:</b> {@code effect} (potion effect type name, e.g. {@code speed} or {@code minecraft:speed})
 * <p><b>Optional parameters:</b> {@code duration} (default 3s), {@code amplifier} (default 0)
 */
public final class ApplyStatusMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
        if (!damageEvent.getDamager().equals(player)) return false;
        if (!(damageEvent.getEntity() instanceof LivingEntity target)) return false;

        String effectName = (String) params.getOrDefault("effect", "");
        if (effectName.isBlank()) return false;
        NamespacedKey effectKey = NamespacedKey.fromString(effectName.toLowerCase());
        if (effectKey == null) return false;
        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(effectKey);
        if (type == null) return false;

        int duration = ((Number) params.getOrDefault("duration", 3.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        target.addPotionEffect(new PotionEffect(type, duration, amplifier));
        return true;
    }
}
