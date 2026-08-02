package io.github.chasehuegel.skilling.engine.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry for {@code SkillMechanic} implementations.
 *
 * <p>Mechanics are executable actions triggered by abilities
 * (e.g. yield_multiplier, chain_break). Registered during
 * {@code onEnable()} under kebab-case keys like {@code core:yield_multiplier}.
 *
 * <p>YAML usage: {@code type: "core:yield_multiplier"}
 */
public final class MechanicRegistry {

    private final Map<String, Supplier<Object>> registry = new ConcurrentHashMap<>();
    private final Map<String, List<String>> paramNames = new ConcurrentHashMap<>();

    /**
     * Registers a mechanic class under the given key.
     *
     * @param key   the registry key
     * @param clazz the mechanic class; must have a no-arg constructor
     */
    public void register(String key, Class<?> clazz) {
        register(key, clazz, List.of());
    }

    /**
     * Registers a mechanic class under the given key with its parameter names.
     *
     * @param key        the registry key
     * @param clazz      the mechanic class; must have a no-arg constructor
     * @param paramNames the list of supported parameter names
     */
    public void register(String key, Class<?> clazz, List<String> paramNames) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Mechanic already registered: " + key);
        }
        this.paramNames.put(key, paramNames);
        registry.put(key, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate mechanic: " + key, e);
            }
        });
    }

    /**
     * Retrieves (and instantiates) a mechanic by its registry key.
     *
     * @param key the registry key
     * @return a new instance of the mechanic, or null if not registered
     */
    public Object create(String key) {
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
     * @return list of parameter names, or empty list if unknown
     */
    public List<String> getParameterNames(String key) {
        return paramNames.getOrDefault(key, List.of());
    }

    /**
     * Returns a map of all mechanic keys to their parameter names.
     *
     * @return map of key -> parameter name list
     */
    public Map<String, List<String>> getAllParameterNames() {
        return Map.copyOf(paramNames);
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
     * Returns all registered mechanic keys.
     *
     * @return set of registry keys
     */
    public java.util.Set<String> keys() {
        return registry.keySet();
    }
}