package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Temporarily modifies a player's attribute (e.g. {@code minecraft:max_health}, {@code minecraft:movement_speed})
 * for a specified duration using a transient {@link AttributeModifier}.
 *
 * <p><b>YAML key:</b> {@code core:modify_attribute}
 * <p><b>Required parameters:</b> {@code attribute} (namespaced key, e.g. {@code minecraft:movement_speed},
 * or a legacy numeric attribute ID, e.g. {@code 4} for Movement Speed)
 * <p><b>Optional parameters:</b> {@code amount} (modifier value), {@code duration} (default 5s),
 * {@code uuid} (stable modifier UUID so repeated activations refresh instead of stacking)
 *
 * <p>Numeric IDs: 1=MAX_HEALTH, 2=FOLLOW_RANGE, 3=KNOCKBACK_RESISTANCE, 4=MOVEMENT_SPEED,
 * 5=FLYING_SPEED, 6=ARMOR, 7=ARMOR_TOUGHNESS, 8=ATTACK_DAMAGE, 9=ATTACK_SPEED, 10=LUCK.
 * Numeric IDs are deprecated in favor of namespaced keys. Unknown keys throw
 * {@link IllegalArgumentException} per the fail-fast convention.
 */
public final class ModifyAttributeMechanic implements SkillMechanic {

    private static final Logger LOGGER = Logger.getLogger(ModifyAttributeMechanic.class.getName());

    /**
     * Legacy numeric attribute IDs, accepted for backward compatibility only.
     * Immutable (design decision, ISSUE-301): a deprecated compatibility shim,
     * not author-facing content.
     */
    private static final Map<Integer, String> LEGACY_ATTRIBUTE_KEYS = Map.of(
            1, "minecraft:max_health",
            2, "minecraft:follow_range",
            3, "minecraft:knockback_resistance",
            4, "minecraft:movement_speed",
            5, "minecraft:flying_speed",
            6, "minecraft:armor",
            7, "minecraft:armor_toughness",
            8, "minecraft:attack_damage",
            9, "minecraft:attack_speed",
            10, "minecraft:luck"
    );

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Attribute attribute = resolveAttribute(params.get("attribute"));
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue();
        if (amount == 0) return false;

        return AttributeModifierHelper.applyTransient(
                player, attribute, AttributeModifierHelper.resolveUuid(params.get("uuid")),
                "skilling_modifier", amount, duration);
    }

    /**
     * Resolves a raw attribute parameter against the live attribute registry.
     *
     * @param rawAttr the raw parameter value (namespaced key or legacy numeric ID)
     * @return the resolved attribute
     * @throws IllegalArgumentException if the parameter is null or unknown
     */
    static Attribute resolveAttribute(Object rawAttr) {
        return resolveAttribute(rawAttr, key -> Registry.ATTRIBUTE.get(key));
    }

    /**
     * Resolves a raw attribute parameter through the supplied namespaced lookup.
     *
     * <p>Namespaced string keys (containing {@code :}) are parsed directly;
     * anything else is treated as a legacy numeric ID. Null input, malformed
     * keys, and keys missing from the lookup fail fast.
     *
     * @param rawAttr the raw parameter value
     * @param lookup  maps a namespaced key to its attribute
     * @return the resolved attribute
     * @throws IllegalArgumentException if the parameter is null or unknown
     */
    static Attribute resolveAttribute(Object rawAttr, Function<NamespacedKey, Attribute> lookup) {
        Attribute attribute = lookup.apply(parseAttributeKey(rawAttr));
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute key: " + rawAttr);
        }
        return attribute;
    }

    /**
     * Parses a raw attribute parameter into its namespaced key. Namespaced
     * strings are parsed directly; anything else is treated as a legacy numeric
     * ID. Shared with load-time validation so the key grammar is defined once.
     *
     * @param rawAttr the raw parameter value
     * @return the namespaced key
     * @throws IllegalArgumentException if the parameter is null or malformed
     */
    static NamespacedKey parseAttributeKey(Object rawAttr) {
        if (rawAttr == null) {
            throw new IllegalArgumentException("Attribute parameter is required");
        }
        if (rawAttr instanceof String s && s.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(s);
            if (key == null) {
                throw new IllegalArgumentException("Unknown attribute key: " + rawAttr);
            }
            return key;
        }
        int id;
        if (rawAttr instanceof Number n) {
            id = n.intValue();
        } else {
            try {
                id = Integer.parseInt(rawAttr.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unknown attribute key: " + rawAttr);
            }
        }
        String keyName = LEGACY_ATTRIBUTE_KEYS.get(id);
        if (keyName == null) {
            throw new IllegalArgumentException("Unknown attribute key: " + rawAttr);
        }
        LOGGER.warning("Deprecated numeric attribute ID " + id + " used; prefer the namespaced key '" + keyName + "'");
        return NamespacedKey.fromString(keyName);
    }
}
