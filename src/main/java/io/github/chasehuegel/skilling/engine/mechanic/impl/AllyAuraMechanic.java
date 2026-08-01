package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;
import java.util.function.Function;

/**
 * Applies a potion effect to the casting player and all nearby players (allies)
 * within a radius. Hostile mobs are never affected.
 *
 * <p><b>YAML key:</b> {@code core:ally_aura}
 * <p><b>Required parameters:</b> {@code effect} (namespaced key, e.g. {@code minecraft:regeneration})
 * <p><b>Optional parameters:</b> {@code radius} (default 8.0, clamped to [0, 32]),
 * {@code duration} (default 5s), {@code amplifier} (default 0)
 *
 * <p>Unlike {@link FieldAuraMechanic}, which iterates every nearby living entity
 * and may inadvertently buff hostile mobs, this mechanic only targets the results
 * of {@code getNearbyPlayers()}. Item economy (e.g. an ingredient cost per cast)
 * is enforced by the ability's {@code requirements.items} with {@code action: cost}
 * following the Check/Execute/Consume pattern.
 */
public final class AllyAuraMechanic implements SkillMechanic {

    /** Matches Bukkit's entity-search radius cap. */
    private static final double MAX_RADIUS = 32.0;

    /** Parsed aura configuration shared with {@link #resolveParams(Map, Function)}. */
    record AuraConfig(double radius, int durationTicks, int amplifier) {}

    /** Fully-resolved aura ready for application. */
    record AuraParams(PotionEffect effect, double radius) {}

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        AuraParams aura = resolveParams(params);
        player.addPotionEffect(aura.effect());
        for (Player ally : player.getLocation().getNearbyPlayers(aura.radius())) {
            if (!ally.getUniqueId().equals(player.getUniqueId())) {
                ally.addPotionEffect(aura.effect());
            }
        }
        return true;
    }

    /**
     * Resolves aura parameters against the live potion effect registry.
     *
     * @param params the mechanic parameters
     * @return the resolved aura
     * @throws IllegalArgumentException if {@code effect} is missing or unknown
     */
    static AuraParams resolveParams(Map<String, Object> params) {
        return resolveParams(params, key -> Registry.POTION_EFFECT_TYPE.get(key));
    }

    /**
     * Resolves aura parameters through the supplied namespaced lookup.
     *
     * @param params the mechanic parameters
     * @param lookup maps a namespaced key to its potion effect type
     * @return the resolved aura
     * @throws IllegalArgumentException if {@code effect} is missing or unknown
     */
    static AuraParams resolveParams(Map<String, Object> params, Function<NamespacedKey, PotionEffectType> lookup) {
        AuraConfig config = parseConfig(params);
        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"), lookup);
        return new AuraParams(new PotionEffect(type, config.durationTicks(), config.amplifier()), config.radius());
    }

    /**
     * Parses the numeric aura parameters, converting duration from seconds to ticks.
     *
     * @param params the mechanic parameters
     * @return the parsed aura configuration
     */
    static AuraConfig parseConfig(Map<String, Object> params) {
        double radius = clampRadius(((Number) params.getOrDefault("radius", 8.0)).doubleValue());
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        return new AuraConfig(radius, duration, amplifier);
    }

    /**
     * Clamps a radius to Bukkit's [0, 32] entity-search bounds.
     *
     * @param radius the requested radius
     * @return the clamped radius
     */
    static double clampRadius(double radius) {
        return Math.max(0.0, Math.min(radius, MAX_RADIUS));
    }
}
