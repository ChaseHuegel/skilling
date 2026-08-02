package io.github.chasehuegel.skilling.engine.registry;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry for {@code SkillMechanic} implementations.
 *
 * <p>Mechanics are executable actions triggered by abilities
 * (e.g. yield_multiplier, chain_break). Registered during
 * {@code onEnable()} under kebab-case keys like {@code core:yield_multiplier}.
 *
 * <p>YAML usage: {@code type: "core:yield_multiplier"}
 *
 * <p>Registration is fail-fast: the class must implement {@link SkillMechanic} and
 * expose a public no-arg constructor, both checked at {@link #register} time. All
 * read methods return immutable snapshots, and caller-supplied parameter lists are
 * copied defensively so registry state can never be mutated through its API.
 */
public final class MechanicRegistry {

    private final Map<String, Supplier<? extends SkillMechanic>> registry = new ConcurrentHashMap<>();
    private final Map<String, List<String>> paramNames = new ConcurrentHashMap<>();

    /**
     * Registers a mechanic class under the given key.
     *
     * @param key   the registry key
     * @param clazz the mechanic class; must implement {@link SkillMechanic} and have a public no-arg constructor
     * @throws IllegalArgumentException if the key is taken or the class is invalid
     */
    public void register(String key, Class<? extends SkillMechanic> clazz) {
        register(key, clazz, List.of());
    }

    /**
     * Registers a mechanic class under the given key with its parameter names.
     *
     * @param key        the registry key
     * @param clazz      the mechanic class; must implement {@link SkillMechanic} and have a public no-arg constructor
     * @param paramNames the supported parameter names (stored defensively)
     * @throws IllegalArgumentException if the key is taken or the class is invalid
     */
    public void register(String key, Class<? extends SkillMechanic> clazz, List<String> paramNames) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Mechanic already registered: " + key);
        }
        RegistrySupport.requirePublicNoArgConstructor(key, clazz);
        this.paramNames.put(key, List.copyOf(paramNames));
        registry.put(key, () -> instantiate(key, clazz));
    }

    private static SkillMechanic instantiate(String key, Class<? extends SkillMechanic> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate mechanic: " + key, e);
        }
    }

    /**
     * Retrieves (and instantiates) a mechanic by its registry key.
     *
     * @param key the registry key
     * @return a new instance of the mechanic, or null if not registered
     */
    public SkillMechanic create(String key) {
        var supplier = registry.get(key);
        return supplier != null ? supplier.get() : null;
    }

    /**
     * Returns true if the given key is registered.
     *
     * @param key the registry key
     * @return true if registered
     */
    public boolean contains(String key) {
        return registry.containsKey(key);
    }

    /**
     * Returns the parameter names for a given mechanic key.
     *
     * @param key the registry key
     * @return immutable list of parameter names, or empty list if unknown
     */
    public List<String> getParameterNames(String key) {
        return List.copyOf(paramNames.getOrDefault(key, List.of()));
    }

    /**
     * Returns an immutable snapshot of all mechanic keys to their parameter names.
     *
     * @return map of key -> immutable parameter name list
     */
    public Map<String, List<String>> getAllParameterNames() {
        Map<String, List<String>> snapshot = new HashMap<>();
        paramNames.forEach((key, names) -> snapshot.put(key, List.copyOf(names)));
        return Map.copyOf(snapshot);
    }

    /**
     * Clears all registered mechanics.
     */
    public void clear() {
        registry.clear();
        paramNames.clear();
    }

    /**
     * Returns the number of registered mechanics.
     *
     * @return the number of mechanics
     */
    public int size() {
        return registry.size();
    }

    /**
     * Returns all registered mechanic keys as an immutable snapshot.
     *
     * @return set of registry keys
     */
    public java.util.Set<String> keys() {
        return java.util.Set.copyOf(registry.keySet());
    }
}
