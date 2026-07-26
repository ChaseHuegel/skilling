package io.github.chasehuegel.skilling.engine.registry;

import java.util.HashMap;
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

    private final Map<String, Supplier<Object>> registry = new HashMap<>();

    /**
     * Registers a mechanic class under the given key.
     *
     * @param key   the registry key
     * @param clazz the mechanic class; must have a no-arg constructor
     */
    public void register(String key, Class<?> clazz) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Mechanic already registered: " + key);
        }
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
     * Clears all registered mechanics.
     */
    public void clear() {
        registry.clear();
    }

    /**
     * Returns the number of registered mechanics.
     *
     * @return the number of mechanics
     */
    public int size() {
        return registry.size();
    }
}