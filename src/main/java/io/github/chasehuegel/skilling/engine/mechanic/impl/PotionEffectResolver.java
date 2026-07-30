package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

final class PotionEffectResolver {

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

    static PotionEffectType resolve(Object rawEffect) {
        if (rawEffect == null) return null;
        int id;
        if (rawEffect instanceof Number n) {
            id = n.intValue();
        } else {
            try {
                id = Integer.parseInt(rawEffect.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        String effectName = null;
        for (int i = 0; i < LEGACY_IDS.length; i++) {
            if (LEGACY_IDS[i] == id) {
                effectName = LEGACY_NAMES[i];
                break;
            }
        }
        if (effectName == null) return null;
        NamespacedKey effectKey = NamespacedKey.fromString(effectName);
        if (effectKey == null) return null;
        return Registry.POTION_EFFECT_TYPE.get(effectKey);
    }

    private PotionEffectResolver() {}
}
