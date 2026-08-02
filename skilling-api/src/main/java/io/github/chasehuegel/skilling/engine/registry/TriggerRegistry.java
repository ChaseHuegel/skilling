package io.github.chasehuegel.skilling.engine.registry;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import java.util.concurrent.ConcurrentHashMap;
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
 *
 * <p>Registration is fail-fast: the class must implement {@link SkillTrigger} and
 * expose a public no-arg constructor, both checked at {@link #register} time. All
 * read methods return immutable snapshots.
 */
public final class TriggerRegistry {

    private final Map<String, Supplier<? extends SkillTrigger>> registry = new ConcurrentHashMap<>();

    /**
     * Registers a trigger class under the given key.
     *
     * @param key   the registry key
     * @param clazz the trigger class; must implement {@link SkillTrigger} and have a public no-arg constructor
     * @throws IllegalArgumentException if the key is taken or the class is invalid
     */
    public void register(String key, Class<? extends SkillTrigger> clazz) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Trigger already registered: " + key);
        }
        RegistrySupport.requirePublicNoArgConstructor(key, clazz);
        registry.put(key, () -> instantiate(key, clazz));
    }

    private static SkillTrigger instantiate(String key, Class<? extends SkillTrigger> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate trigger: " + key, e);
        }
    }

    /**
     * Retrieves (and instantiates) a trigger by its registry key.
     *
     * @param key the registry key
     * @return a new instance of the trigger, or null if not registered
     */
    public SkillTrigger create(String key) {
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
     * Returns all registered trigger keys as an immutable snapshot.
     *
     * @return set of registry keys
     */
    public java.util.Set<String> keys() {
        return java.util.Set.copyOf(registry.keySet());
    }
}
