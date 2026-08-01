package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Shared logic for attribute-modifier mechanics: resolving the modifier UUID
 * (stable per ability when configured, random otherwise) and applying a
 * transient modifier that replaces any existing modifier carrying the same
 * UUID before scheduling its removal.
 *
 * <p>Every attribute-modifier mechanic delegates to
 * {@link #applyTransient(Player, Attribute, UUID, String, double, int)} so the
 * replace-not-stack behavior stays consistent across mechanics.
 */
final class AttributeModifierHelper {

    private AttributeModifierHelper() {}

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
     * @param player        the target player
     * @param attribute     the attribute to modify
     * @param uuid          the modifier UUID (stable per ability when configured)
     * @param modifierName  the modifier name
     * @param amount        the additive modifier amount
     * @param durationSeconds the modifier lifetime in seconds
     * @return true if a modifier was applied
     */
    static boolean applyTransient(Player player, Attribute attribute, UUID uuid, String modifierName,
                                  double amount, int durationSeconds) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return false;

        AttributeModifier existing = inst.getModifier(uuid);
        if (existing != null) {
            inst.removeModifier(existing);
        }

        var modifier = new AttributeModifier(uuid, modifierName, amount, AttributeModifier.Operation.ADD_NUMBER);
        inst.addTransientModifier(modifier);
        player.getScheduler().runDelayed(
                io.github.chasehuegel.skilling.Skilling.getInstance(),
                t -> inst.removeModifier(modifier),
                null,
                durationSeconds * 20L
        );
        return true;
    }
}
