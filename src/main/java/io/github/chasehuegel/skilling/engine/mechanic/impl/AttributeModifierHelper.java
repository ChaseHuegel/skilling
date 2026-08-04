package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared logic for attribute-modifier mechanics: resolving the modifier UUID
 * (stable per ability when configured, random otherwise) and applying a
 * transient modifier that replaces any existing modifier carrying the same
 * UUID before scheduling its removal.
 *
 * <p>Every attribute-modifier mechanic delegates to
 * {@link #applyTransient(Player, Attribute, UUID, String, double, int)} so the
 * replace-not-stack behavior stays consistent across mechanics.
 *
 * <p>A refresh cancels the previously scheduled removal (tracked per player and
 * modifier UUID) instead of letting the stale task fire and cut the new buff
 * short. Removal tasks run through Paper's entity scheduler, which retires them
 * when the player despawns (e.g. on quit); the retired callback drops the
 * tracked entry so nothing leaks for a player who logs out mid-buff.
 */
final class AttributeModifierHelper {

    private static final ConcurrentMap<BuffKey, ScheduledTask> PENDING_REMOVALS = new ConcurrentHashMap<>();

    private AttributeModifierHelper() {}

    /** Identifies a scheduled removal by the player and the modifier UUID. */
    private record BuffKey(UUID playerId, UUID modifierId) {}

    /**
     * Resolves the modifier UUID from an optional {@code uuid} parameter value.
     *
     * <p>When the raw value is null the modifier gets a fresh random UUID,
     * preserving the legacy no-{@code uuid} behavior. A malformed UUID string
     * fails fast.
     *
     * @param rawUuid the raw {@code uuid} parameter value, or null
     * @return the resolved UUID
     * @throws IllegalArgumentException if the raw value is not a valid UUID
     */
    static UUID resolveUuid(Object rawUuid) {
        if (rawUuid == null) return UUID.randomUUID();
        String s = rawUuid.toString();
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid uuid for attribute modifier: " + s, e);
        }
    }

    /**
     * Applies a transient attribute modifier under the given UUID, replacing any
     * existing modifier with the same UUID first so repeated activations refresh
     * rather than stack. The modifier is removed after the duration expires.
     *
     * @param player          the target player
     * @param attribute       the attribute to modify
     * @param uuid            the modifier UUID (stable per ability when configured)
     * @param modifierName    the modifier name
     * @param amount          the additive modifier amount
     * @param durationSeconds the modifier lifetime in seconds
     * @return true if a modifier was applied
     */
    static boolean applyTransient(Player player, Attribute attribute, UUID uuid, String modifierName,
                                  double amount, int durationSeconds) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return false;

        BuffKey key = new BuffKey(player.getUniqueId(), uuid);
        ScheduledTask previous = PENDING_REMOVALS.remove(key);
        if (previous != null && !previous.isCancelled()) {
            previous.cancel();
        }

        AttributeModifier existing = inst.getModifier(uuid);
        if (existing != null) {
            inst.removeModifier(existing);
        }

        var modifier = new AttributeModifier(uuid, modifierName, amount, AttributeModifier.Operation.ADD_NUMBER);
        inst.addTransientModifier(modifier);

        // The retired callback needs the exact task to drop only its own map
        // entry, so capture it through a one-element array (not effectively final
        // until runDelayed returns).
        ScheduledTask[] removal = { null };
        ScheduledTask task = player.getScheduler().runDelayed(
                Skilling.getInstance(),
                t -> {
                    PENDING_REMOVALS.remove(key, t);
                    inst.removeModifier(uuid);
                },
                () -> PENDING_REMOVALS.remove(key, removal[0]),
                durationSeconds * 20L
        );
        removal[0] = task;
        if (task != null) {
            PENDING_REMOVALS.put(key, task);
        }
        return true;
    }
}
