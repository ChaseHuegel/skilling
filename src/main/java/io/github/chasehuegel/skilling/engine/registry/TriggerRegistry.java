package io.github.chasehuegel.skilling.engine.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry for {@code SkillTrigger} implementations.
 *
 * <p>Triggers hook into Paper events to grant XP or activate abilities
 * (e.g. block_break, entity_damage). Registered during {@code onEnable()}
 * under kebab-case keys like {@code block_break}.
 *
 * <p>YAML usage: {@code trigger: "block_break"}
 */
public final class TriggerRegistry {

    private final Map<String, Supplier<Object>> registry = new HashMap<>();

    /**
     * Registers a trigger class under the given key.
     *
     * @param key   the registry key
     * @param clazz the trigger class; must have a no-arg constructor
     */
    public void register(String key, Class<?> clazz) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Trigger already registered: " + key);
        }
        registry.put(key, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate trigger: " + key, e);
            }
        });
    }

    /**
     * Retrieves (and instantiates) a trigger by its registry key.
     *
     * @param key the registry key
     * @return a new instance of the trigger, or null if not registered
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
     * Clears all registered triggers.
     */
    public void clear() {
        registry.clear();
    }

    /**
     * Returns the number of registered triggers.
     *
     * @return the number of triggers
     */
    public int size() {
        return registry.size();
    }

    /**
     * Returns all registered trigger keys.
     *
     * @return set of registry keys
     */
    public java.util.Set<String> keys() {
        return registry.keySet();
    }
}