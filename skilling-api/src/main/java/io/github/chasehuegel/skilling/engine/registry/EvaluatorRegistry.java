package io.github.chasehuegel.skilling.engine.registry;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registry for {@code ParameterEvaluator} instances.
 *
 * <p>Evaluators are stateless math processors that calculate dynamic values
 * based on a player's current level (e.g. linear, milestone, constant).
 * Registered during {@code onEnable()} under kebab-case keys like {@code linear}.
 *
 * <p>YAML usage: {@code parameters: { yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } } }}
 */
public final class EvaluatorRegistry {

    private final Map<String, Object> registry = new ConcurrentHashMap<>();

    /**
     * Registers an evaluator class under the given key.
     *
     * @param key   the registry key
     * @param clazz the evaluator class; must implement {@link ParameterEvaluator} and have a public no-arg constructor
     * @throws IllegalArgumentException if the key is taken or the class is invalid
     */
    public void register(String key, Class<? extends ParameterEvaluator> clazz) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Evaluator already registered: " + key);
        }
        RegistrySupport.requirePublicNoArgConstructor(key, clazz);
        registry.put(key, clazz);
    }

    /**
     * Registers an evaluator instance under the given key.
     *
     * @param key       the registry key
     * @param evaluator the evaluator instance
     * @throws IllegalArgumentException if the key is taken
     */
    public void register(String key, ParameterEvaluator evaluator) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Evaluator already registered: " + key);
        }
        registry.put(key, evaluator);
    }

    /**
     * Registers an evaluator under the given key, either a {@link ParameterEvaluator}
     * instance or a class that will be instantiated fresh per parse.
     *
     * @param key       the registry key
     * @param evaluator the evaluator instance or {@code Class<? extends ParameterEvaluator>}
     * @throws IllegalArgumentException if the key is taken or the value is neither a {@link ParameterEvaluator} nor a {@code ParameterEvaluator} class
     * @deprecated Use {@link #register(String, ParameterEvaluator)} or {@link #register(String, Class)}.
     */
    @Deprecated
    public void register(String key, Object evaluator) {
        if (evaluator instanceof ParameterEvaluator pe) {
            register(key, pe);
        } else if (evaluator instanceof Class<?> clazz) {
            if (!ParameterEvaluator.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(
                        "Evaluator must be a ParameterEvaluator or a Class<? extends ParameterEvaluator>, got: " + evaluator);
            }
            @SuppressWarnings("unchecked")
            Class<? extends ParameterEvaluator> typed = (Class<? extends ParameterEvaluator>) clazz;
            register(key, typed);
        } else {
            throw new IllegalArgumentException(
                    "Evaluator must be a ParameterEvaluator or a Class, got: " + evaluator);
        }
    }

    /**
     * Retrieves a usable {@link ParameterEvaluator} for the given key, instantiating
     * registered classes via their public no-arg constructor.
     *
     * @param key the registry key
     * @return the evaluator
     * @throws IllegalArgumentException if the key is unregistered or the class cannot be instantiated
     */
    public ParameterEvaluator create(String key) {
        Object value = registry.get(key);
        if (value instanceof ParameterEvaluator pe) return pe;
        if (value instanceof Class<?> clazz) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (!(instance instanceof ParameterEvaluator pe)) {
                    throw new IllegalArgumentException(
                            "Registered evaluator class " + clazz.getName() + " is not a ParameterEvaluator");
                }
                return pe;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to instantiate evaluator: " + key, e);
            }
        }
        throw new IllegalArgumentException("Evaluator not registered: " + key);
    }

    /**
     * Returns the raw registered value (instance or class) for the given key.
     *
     * @param key the registry key
     * @return the evaluator instance or class, or null if not registered
     * @deprecated Use {@link #create(String)} for a usable evaluator.
     */
    @Deprecated
    public Object get(String key) {
        return registry.get(key);
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
     * Clears all registered evaluators.
     */
    public void clear() {
        registry.clear();
    }

    /**
     * Returns the number of registered evaluators.
     *
     * @return the number of evaluators
     */
    public int size() {
        return registry.size();
    }

    /**
     * Returns all registered evaluator keys as an immutable snapshot.
     *
     * @return set of registry keys
     */
    public java.util.Set<String> keys() {
        return java.util.Set.copyOf(registry.keySet());
    }
}
