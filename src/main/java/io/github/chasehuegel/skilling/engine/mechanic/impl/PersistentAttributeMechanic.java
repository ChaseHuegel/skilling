package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Permanently adjusts a player attribute (for example {@code minecraft:max_health})
 * by a level-scaled amount, replacing any prior modifier with the same stable
 * UUID instead of stacking.
 *
 * <p>The modifier is <em>transient</em>: it is added with
 * {@link AttributeInstance#addTransientModifier} and never saved to NBT, so the
 * clean-unplug pillar holds — removing the plugin (or restarting without it)
 * returns the player's attributes to vanilla values. Because it implements
 * {@link UnlockMechanic}, the engine re-runs it on player join, after
 * {@code /skills reload}, and when the owning skill's level changes, evaluating
 * {@code amount} at the player's current level.
 *
 * <p>Execution is idempotent per amount: an existing modifier with the same
 * UUID and amount is left untouched (a no-op), a different amount replaces the
 * modifier, and a non-positive amount removes the modifier entirely. After any
 * {@code minecraft:max_health} change the player's current health is clamped to
 * the new maximum so a lowered cap cannot leave health above it.
 *
 * <p><b>YAML key:</b> {@code core:persistent_attribute}
 * <br>Params: {@code attribute} (namespaced key, e.g. {@code minecraft:max_health}),
 * {@code amount} (level-scaled evaluator), {@code uuid} (required stable modifier
 * UUID constant; repeated executions with the same UUID refresh, never stack)
 */
public record PersistentAttributeMechanic() implements UnlockMechanic {

    /** Namespaced-key namespace shared by every persistent attribute modifier. */
    public static final String MODIFIER_NAMESPACE = "skilling";

    /** Namespaced-key value prefix shared by every persistent attribute modifier. */
    public static final String MODIFIER_KEY_PREFIX = "persistent";

    /**
     * Attributes a persistent modifier has ever been applied to, so the strip
     * pass can enumerate the player's attribute instances without walking the
     * whole attribute registry. Bounded by the number of attributes (a handful)
     * and never pruned: a no-op attribute visit costs one {@code getAttribute}.
     */
    private static final Set<Attribute> TOUCHED_ATTRIBUTES = ConcurrentHashMap.newKeySet();

    /**
     * Builds the modifier key for a stable per-ability UUID, so the key encodes
     * both the plugin marker and the ability identity that replace-not-stack
     * relies on.
     *
     * @param uuid the stable modifier UUID
     * @return the namespaced modifier key
     */
    static NamespacedKey modifierKey(UUID uuid) {
        return NamespacedKey.fromString(MODIFIER_NAMESPACE + ":" + MODIFIER_KEY_PREFIX + "_" + uuid);
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Object rawUuid = params.get("uuid");
        if (rawUuid == null) return false;
        Attribute attribute = ModifyAttributeMechanic.resolveAttribute(params.get("attribute"));
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        return applyPersistent(player, attribute, AttributeModifierHelper.resolveUuid(rawUuid), amount);
    }

    /**
     * Applies a persistent transient modifier under the given UUID, replacing any
     * existing modifier that carries it and leaving an identical one untouched.
     *
     * @param player    the target player
     * @param attribute the attribute to modify
     * @param uuid      the stable modifier UUID
     * @param amount    the additive modifier amount; non-positive removes instead
     * @return true if the modifier state changed (added or replaced), false when
     *         the attribute is absent, the amount is non-positive, or the current
     *         modifier already matches the amount
     */
    static boolean applyPersistent(Player player, Attribute attribute, UUID uuid, double amount) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return false;
        TOUCHED_ATTRIBUTES.add(attribute);
        NamespacedKey key = modifierKey(uuid);
        AttributeModifier existing = inst.getModifier(key);
        if (existing != null) {
            if (amount == existing.getAmount()) return false;
            inst.removeModifier(key);
        }
        if (amount <= 0) return false;
        inst.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        if (attribute == Attribute.MAX_HEALTH) {
            clampHealth(player);
        }
        return true;
    }

    /**
     * Removes every persistent attribute modifier (marker key in the
     * {@code skilling:persistent_*} namespace) from the player, then clamps
     * health to the new maximum. Called by the engine before re-applying active
     * persistent mechanics during join/reload/level reconciliation, so a
     * de-level, reset, or removed skill recomputes from scratch instead of
     * leaking a stale bonus.
     *
     * @param player the player to strip
     */
    public static void stripPersistentModifiers(Player player) {
        boolean touchedMaxHealth = false;
        for (Attribute attribute : List.copyOf(TOUCHED_ATTRIBUTES)) {
            AttributeInstance inst = player.getAttribute(attribute);
            if (inst == null) continue;
            for (AttributeModifier modifier : inst.getModifiers().stream()
                    .filter(m -> MODIFIER_NAMESPACE.equals(m.getKey().getNamespace())
                            && m.getKey().getKey().startsWith(MODIFIER_KEY_PREFIX + "_"))
                    .toList()) {
                inst.removeModifier(modifier);
                if (attribute == Attribute.MAX_HEALTH) touchedMaxHealth = true;
            }
        }
        if (touchedMaxHealth) clampHealth(player);
    }

    /**
     * Clamps the player's current health to the value of the max-health
     * attribute, so removing or lowering a {@code minecraft:max_health} modifier
     * cannot leave health above the new cap.
     *
     * @param player the player to clamp
     */
    public static void clampHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }
}