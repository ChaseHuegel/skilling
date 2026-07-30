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
 * <p><b>Required parameters:</b> {@code effect} (legacy numeric potion effect ID, e.g. {@code 1} for Speed)
 * <p><b>Optional parameters:</b> {@code duration} (default 3s), {@code amplifier} (default 0)
 *
 * <p>Numeric IDs are mapped to namespaced keys internally for Paper 1.21+ compatibility.
 */
public final class ApplyStatusMechanic implements SkillMechanic {

    private static final int[] LEGACY_IDS = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39
    };

    private static final String[] LEGACY_NAMES = {
        "minecraft:speed", "minecraft:slowness", "minecraft:haste", "minecraft:mining_fatigue",
        "minecraft:strength", "minecraft:instant_health", "minecraft:instant_damage",
        "minecraft:jump_boost", "minecraft:nausea", "minecraft:regeneration",
        "minecraft:resistance", "minecraft:fire_resistance", "minecraft:water_breathing",
        "minecraft:invisibility", "minecraft:blindness", "minecraft:night_vision",
        "minecraft:hunger", "minecraft:weakness", "minecraft:poison", "minecraft:wither",
        "minecraft:health_boost", "minecraft:absorption", "minecraft:saturation",
        "minecraft:glowing", "minecraft:levitation", "minecraft:luck", "minecraft:unluck",
        "minecraft:slow_falling", "minecraft:conduit_power", "minecraft:dolphins_grace",
        "minecraft:bad_omen", "minecraft:hero_of_the_village", "minecraft:darkness",
        "minecraft:trial_omen", "minecraft:raid_omen", "minecraft:wind_charged",
        "minecraft:weaving", "minecraft:oozing", "minecraft:infested"
    };

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
        if (!damageEvent.getDamager().equals(player)) return false;
        if (!(damageEvent.getEntity() instanceof LivingEntity target)) return false;

        Object rawEffect = params.get("effect");
        if (rawEffect == null) return false;
        int id;
        if (rawEffect instanceof Number n) {
            id = n.intValue();
        } else {
            try {
                id = Integer.parseInt(rawEffect.toString());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        String effectName = null;
        for (int i = 0; i < LEGACY_IDS.length; i++) {
            if (LEGACY_IDS[i] == id) {
                effectName = LEGACY_NAMES[i];
                break;
            }
        }
        if (effectName == null) return false;
        NamespacedKey effectKey = NamespacedKey.fromString(effectName);
        if (effectKey == null) return false;
        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(effectKey);
        if (type == null) return false;

        int duration = ((Number) params.getOrDefault("duration", 3.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        target.addPotionEffect(new PotionEffect(type, duration, amplifier));
        return true;
    }
}
