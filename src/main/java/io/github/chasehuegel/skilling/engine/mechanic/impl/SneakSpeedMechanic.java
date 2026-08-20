package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a movement-speed modifier that only lasts while the player sneaks.
 *
 * <p>The modifier is applied when the {@code sneak} trigger fires (sneak start).
 * The engine strips it on {@link PlayerToggleSneakEvent} release via
 * {@link #strip(Player)}, so the bonus never lingers after the player stands up:
 * no per-tick task and no timed buff left behind.
 *
 * <p><b>YAML key:</b> {@code core:sneak_speed}
 * <br>Params: {@code multiplier} (e.g. 1.25 = 25% faster), {@code uuid} (stable
 * modifier UUID so repeated applications replace instead of stack)
 */
public final class SneakSpeedMechanic implements SkillMechanic {

    /** Modifier UUIDs currently applied per player, for release stripping. */
    private static final Map<UUID, Set<UUID>> ACTIVE = new ConcurrentHashMap<>();

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;
        UUID uuid = AttributeModifierHelper.resolveUuid(params.get("uuid"));
        AttributeInstance inst = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst == null) return false;
        double amount = inst.getBaseValue() * (multiplier - 1.0);
        removeIfPresent(inst, uuid);
        inst.addTransientModifier(new AttributeModifier(uuid, "skilling_sneak_speed", amount,
                AttributeModifier.Operation.ADD_NUMBER));
        ACTIVE.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(uuid);
        return true;
    }

    /**
     * Removes every sneak speed modifier currently applied to a player. Called
     * by the engine when the player stops sneaking.
     *
     * @param player the player who stopped sneaking
     */
    public static void strip(Player player) {
        Set<UUID> uuids = ACTIVE.remove(player.getUniqueId());
        if (uuids == null) return;
        AttributeInstance inst = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst != null) {
            for (UUID uuid : uuids) {
                removeIfPresent(inst, uuid);
            }
        }
    }

    private static void removeIfPresent(AttributeInstance inst, UUID uuid) {
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing != null) {
            inst.removeModifier(existing);
        }
    }
}
