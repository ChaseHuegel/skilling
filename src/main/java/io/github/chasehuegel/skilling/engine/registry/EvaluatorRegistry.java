package io.github.chasehuegel.skilling.engine.registry;

import java.util.HashMap;
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

    private final Map<String, Object> registry = new HashMap<>();

    /**
     * Registers an evaluator instance under the given key.
     *
     * @param key       the registry key
     * @param evaluator the evaluator instance
     */
    public void register(String key, Object evaluator) {
        if (registry.containsKey(key)) {
            throw new IllegalArgumentException("Evaluator already registered: " + key);
        }
        registry.put(key, evaluator);
    }

    /**
     * Retrieves an evaluator by its registry key.
     *
     * @param key the registry key
     * @return the evaluator instance, or null if not registered
     */
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
}