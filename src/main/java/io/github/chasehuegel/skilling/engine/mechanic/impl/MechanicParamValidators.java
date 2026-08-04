package io.github.chasehuegel.skilling.engine.mechanic.impl;

import java.util.Map;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Load-time validators for string-valued mechanic parameters.
 *
 * <p>Each validator resolves its parameter when the skill YAML is parsed, so a
 * typo (e.g. {@code effect: "minecraft:poisn"}) is rejected with a descriptive
 * {@link IllegalArgumentException} at load time instead of throwing inside an
 * event handler mid-game. Only constant-valued parameters can be validated at
 * load; a missing required string param (effect, attribute) is rejected, while
 * intentionally optional params (radius, chance, duration) and mechanics with an
 * optional string param keep their skip-when-absent behavior. A present numeric
 * param carrying a string (e.g. {@code duration: "3"}) is rejected instead of
 * throwing {@link ClassCastException} at runtime.
 *
 * <p>The potion-effect and attribute key checks are injected by the plugin at
 * startup ({@link #configureLookups}) because the live Bukkit registries cannot
 * initialize in a plain-JUnit JVM. Until configured, those validators skip the
 * key check — but still reject a missing required param — so unit tests stay
 * green, and the dedicated load-validation tests inject deterministic
 * predicates. Material and particle validation needs no registry and always
 * runs.
 */
public final class MechanicParamValidators {

    private static volatile Predicate<NamespacedKey> potionKeyKnown;
    private static volatile Predicate<NamespacedKey> attributeKeyKnown;
    private static volatile Predicate<NamespacedKey> soundKeyKnown;
    private static volatile Predicate<NamespacedKey> particleKeyKnown;

    private MechanicParamValidators() {}

    /**
     * Configures the live registry key checks used by load-time validation.
     *
     * @param potionKeyKnown    tests whether a namespaced potion effect key exists
     * @param attributeKeyKnown tests whether a namespaced attribute key exists
     * @param soundKeyKnown     tests whether a namespaced sound key exists
     * @param particleKeyKnown  tests whether a namespaced particle key exists
     */
    public static void configureLookups(Predicate<NamespacedKey> potionKeyKnown,
                                        Predicate<NamespacedKey> attributeKeyKnown,
                                        Predicate<NamespacedKey> soundKeyKnown,
                                        Predicate<NamespacedKey> particleKeyKnown) {
        MechanicParamValidators.potionKeyKnown = potionKeyKnown;
        MechanicParamValidators.attributeKeyKnown = attributeKeyKnown;
        MechanicParamValidators.soundKeyKnown = soundKeyKnown;
        MechanicParamValidators.particleKeyKnown = particleKeyKnown;
    }

    /**
     * Validates a potion-effect parameter. The parameter is required: a missing
     * effect would throw {@link IllegalArgumentException} inside the mechanic's
     * event handler, so it is rejected here at load instead.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key holding the effect
     * @throws IllegalArgumentException if the effect is missing or unknown
     */
    public static void potionEffect(String context, Map<String, Object> params, String key) {
        if (!params.containsKey(key)) {
            throw new IllegalArgumentException(context + ": missing required parameter '" + key + "'");
        }
        if (potionKeyKnown == null) return;
        Object raw = params.get(key);
        try {
            if (!potionKeyKnown.test(PotionEffectResolver.parseKey(raw))) {
                throw new IllegalArgumentException("Unknown potion effect key: " + raw);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(context + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates an attribute parameter. The parameter is required: a missing
     * attribute would throw inside the mechanic's event handler, so it is
     * rejected here at load instead.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key holding the attribute
     * @throws IllegalArgumentException if the attribute is missing or unknown
     */
    public static void attribute(String context, Map<String, Object> params, String key) {
        if (!params.containsKey(key)) {
            throw new IllegalArgumentException(context + ": missing required parameter '" + key + "'");
        }
        if (attributeKeyKnown == null) return;
        Object raw = params.get(key);
        try {
            if (!attributeKeyKnown.test(ModifyAttributeMechanic.parseAttributeKey(raw))) {
                throw new IllegalArgumentException("Unknown attribute key: " + raw);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(context + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates a material parameter, skipping it when absent or blank.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key holding the material
     * @throws IllegalArgumentException if the material is present but unknown
     */
    public static void material(String context, Map<String, Object> params, String key) {
        if (!params.containsKey(key)) return;
        Object raw = params.get(key);
        String material = raw == null ? "" : String.valueOf(raw);
        if (material.isBlank()) return;
        if (Material.matchMaterial(material) == null) {
            throw new IllegalArgumentException(context + ": unknown material '" + material + "'");
        }
    }

    /**
     * Validates a particle parameter, skipping it when absent or when no registry
     * key check has been configured.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key holding the particle
     * @throws IllegalArgumentException if the particle is present but unknown
     */
    public static void particle(String context, Map<String, Object> params, String key) {
        if (!params.containsKey(key) || particleKeyKnown == null) return;
        Object raw = params.get(key);
        try {
            if (!particleKeyKnown.test(parseNamespacedKey(raw))) {
                throw new IllegalArgumentException("unknown particle '" + raw + "'");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(context + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates a sound parameter, skipping it when absent or when no registry
     * key check has been configured.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued parameters holding the sound
     * @param key     the parameter key holding the sound
     * @throws IllegalArgumentException if the sound is present but unknown
     */
    public static void sound(String context, Map<String, Object> params, String key) {
        if (!params.containsKey(key) || soundKeyKnown == null) return;
        Object raw = params.get(key);
        try {
            if (!soundKeyKnown.test(parseNamespacedKey(raw))) {
                throw new IllegalArgumentException("unknown sound '" + raw + "'");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(context + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses a namespaced identifier value ({@code namespace:key}) for registry
     * key checks, throwing a descriptive error on a missing/invalid value.
     *
     * @param raw the raw parameter value
     * @return the parsed namespaced key
     */
    private static NamespacedKey parseNamespacedKey(Object raw) {
        if (raw == null) throw new IllegalArgumentException("missing value");
        String value = String.valueOf(raw);
        if (value.isBlank()) throw new IllegalArgumentException("blank value");
        NamespacedKey key;
        try {
            key = NamespacedKey.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid namespaced identifier '" + value + "'", e);
        }
        if (key == null) {
            throw new IllegalArgumentException("invalid namespaced identifier '" + value + "'");
        }
        return key;
    }

    /**
     * Validates a non-negative radius parameter, skipping it when absent.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key holding the radius
     * @throws IllegalArgumentException if the radius is present but not a number, or negative
     */
    public static void radius(String context, Map<String, Object> params, String key) {
        Number value = number(params.get(key), context, key);
        if (value != null && value.doubleValue() < 0) {
            throw new IllegalArgumentException(context + ": " + key + " must not be negative, got " + value);
        }
    }

    /**
     * Validates a bounded chance parameter in {@code [0, max]}, skipping it when
     * absent.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key holding the chance
     * @param max     the inclusive upper bound (e.g. 100 for percentages, 1 for a ratio)
     * @throws IllegalArgumentException if the chance is present but not a number, or out of bounds
     */
    public static void chance(String context, Map<String, Object> params, String key, double max) {
        Number value = number(params.get(key), context, key);
        if (value != null && (value.doubleValue() < 0 || value.doubleValue() > max)) {
            throw new IllegalArgumentException(context + ": " + key
                    + " must be between 0 and " + max + ", got " + value);
        }
    }

    /**
     * Validates a non-negative numeric parameter (duration in seconds, cooldown
     * in ticks, etc.), skipping it when absent.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key to validate
     * @throws IllegalArgumentException if the parameter is present but not a number, or negative
     */
    public static void nonNegative(String context, Map<String, Object> params, String key) {
        Number value = number(params.get(key), context, key);
        if (value != null && value.doubleValue() < 0) {
            throw new IllegalArgumentException(context + ": " + key + " must not be negative, got " + value);
        }
    }

    /**
     * Validates a strictly positive numeric parameter, skipping it when absent.
     *
     * @param context the load context (skill/ability) for error messages
     * @param params  the constant-valued mechanic parameters
     * @param key     the parameter key to validate
     * @throws IllegalArgumentException if the parameter is present but not a number, or not positive
     */
    public static void positive(String context, Map<String, Object> params, String key) {
        Number value = number(params.get(key), context, key);
        if (value != null && value.doubleValue() <= 0) {
            throw new IllegalArgumentException(context + ": " + key + " must be positive, got " + value);
        }
    }

    /**
     * Reads a numeric parameter value. A present non-number (e.g. a quoted
     * {@code "3"}) would throw {@link ClassCastException} from the mechanics'
     * {@code (Number)} casts at runtime, so it is rejected here instead.
     *
     * @param value   the raw parameter value (null when absent)
     * @param context the load context for error messages
     * @param key     the parameter key
     * @return the numeric value, or null when absent
     * @throws IllegalArgumentException if the value is present but not a number
     */
    private static Number number(Object value, String context, String key) {
        if (value == null) return null;
        if (value instanceof Number n) return n;
        throw new IllegalArgumentException(context + ": parameter '" + key + "' must be a number, got: " + value);
    }
}
