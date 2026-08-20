package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a potion effect that only lasts while the player sneaks.
 *
 * <p>The effect is applied when the {@code sneak} trigger fires (sneak start).
 * The engine removes it on {@link PlayerToggleSneakEvent} release via
 * {@link #strip(Player)}, so the effect never lingers after the player stands
 * up: no per-tick task and no timed buff left behind. This is the
 * potion-effect sibling of {@link SneakSpeedMechanic}.
 *
 * <p><b>YAML key:</b> {@code core:sneak_effect}
 * <br>Params: {@code effect} (namespaced potion effect key, e.g.
 * {@code minecraft:invisibility}), {@code amplifier} (default 0),
 * {@code duration} (seconds, default 5)
 */
public final class SneakEffectMechanic implements SkillMechanic {

    /** Potion effect types currently applied per player, for release stripping. */
    private static final Map<UUID, Set<PotionEffectType>> ACTIVE = new ConcurrentHashMap<>();

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"));
        if (type == null) return false;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue();
        apply(player, type, amplifier, duration);
        return true;
    }

    /**
     * Applies a sneak-only potion effect to the player and tracks its type so it
     * can be stripped on release.
     *
     * @param player    the sneaking player
     * @param type      the potion effect type to apply
     * @param amplifier the effect amplifier
     * @param duration  the effect duration in seconds
     */
    static void apply(Player player, PotionEffectType type, int amplifier, int duration) {
        player.addPotionEffect(new PotionEffect(type, duration * 20, amplifier));
        ACTIVE.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(type);
    }

    /**
     * Removes every sneak-applied potion effect currently on a player. Called by
     * the engine when the player stops sneaking.
     *
     * @param player the player who stopped sneaking
     */
    public static void strip(Player player) {
        Set<PotionEffectType> types = ACTIVE.remove(player.getUniqueId());
        if (types == null) return;
        for (PotionEffectType type : types) {
            player.removePotionEffect(type);
        }
    }
}
