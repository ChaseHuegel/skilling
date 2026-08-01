package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Resolves the {@code effect} mechanic parameter into a {@link PotionEffectType}.
 *
 * <p>Supports namespaced keys (e.g. {@code "minecraft:poison"}) resolved through
 * {@link Registry#POTION_EFFECT_TYPE}. Legacy numeric IDs are still accepted for
 * backward compatibility but log a deprecation warning. Unknown or malformed keys
 * throw {@link IllegalArgumentException} per the fail-fast convention.
 */
final class PotionEffectResolver {

    private static final Logger LOGGER = Logger.getLogger(PotionEffectResolver.class.getName());

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

    /**
     * Resolves a raw effect parameter against the live potion effect registry.
     *
     * @param rawEffect the raw parameter value (namespaced key or legacy numeric ID)
     * @return the resolved effect type
     * @throws IllegalArgumentException if the parameter is null or unknown
     */
    static PotionEffectType resolve(Object rawEffect) {
        return resolve(rawEffect, key -> Registry.POTION_EFFECT_TYPE.get(key));
    }

    /**
     * Resolves a raw effect parameter through the supplied namespaced lookup.
     *
     * <p>Namespaced string keys (containing {@code :}) are parsed directly;
     * anything else is treated as a legacy numeric ID. Null input, malformed
     * keys, and keys missing from the lookup fail fast.
     *
     * @param rawEffect the raw parameter value
     * @param lookup    maps a namespaced key to its potion effect type
     * @return the resolved effect type
     * @throws IllegalArgumentException if the parameter is null or unknown
     */
    static PotionEffectType resolve(Object rawEffect, Function<NamespacedKey, PotionEffectType> lookup) {
        PotionEffectType type = lookup.apply(parseKey(rawEffect));
        if (type == null) {
            throw new IllegalArgumentException("Unknown potion effect key: " + rawEffect);
        }
        return type;
    }

    private static NamespacedKey parseKey(Object rawEffect) {
        if (rawEffect == null) {
            throw new IllegalArgumentException("Effect parameter is required");
        }
        if (rawEffect instanceof String s && s.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(s);
            if (key == null) {
                throw new IllegalArgumentException("Unknown potion effect key: " + rawEffect);
            }
            return key;
        }
        int id;
        if (rawEffect instanceof Number n) {
            id = n.intValue();
        } else {
            try {
                id = Integer.parseInt(rawEffect.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unknown potion effect key: " + rawEffect);
            }
        }
        String effectName = null;
        for (int i = 0; i < LEGACY_IDS.length; i++) {
            if (LEGACY_IDS[i] == id) {
                effectName = LEGACY_NAMES[i];
                break;
            }
        }
        if (effectName == null) {
            throw new IllegalArgumentException("Unknown potion effect key: " + rawEffect);
        }
        LOGGER.warning("Deprecated numeric potion effect ID " + id + " used; prefer the namespaced key '" + effectName + "'");
        return NamespacedKey.fromString(effectName);
    }

    private PotionEffectResolver() {}
}
