package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;

/**
 * Container holding all three engine registries.
 *
 * <p>Addon developers obtain this via SkillingAPI#getRegistries()
 * to register custom mechanics, triggers, or evaluators.
 */
public final class Registries {

    private final MechanicRegistry mechanicRegistry;
    private final TriggerRegistry triggerRegistry;
    private final EvaluatorRegistry evaluatorRegistry;

    /**
     * Constructs a registries container.
     *
     * @param mechanicRegistry  the mechanic registry
     * @param triggerRegistry   the trigger registry
     * @param evaluatorRegistry the evaluator registry
     */
    public Registries(MechanicRegistry mechanicRegistry, TriggerRegistry triggerRegistry, EvaluatorRegistry evaluatorRegistry) {
        this.mechanicRegistry = mechanicRegistry;
        this.triggerRegistry = triggerRegistry;
        this.evaluatorRegistry = evaluatorRegistry;
    }

    /**
     * Registers a mechanic class under the given key.
     *
     * @param key   the registry key (e.g. "myaddon:lifesteal")
     * @param clazz the mechanic implementation class
     */
    public void registerMechanic(String key, Class<? extends SkillMechanic> clazz) {
        mechanicRegistry.register(key, clazz);
    }

    /**
     * Registers a trigger class under the given key.
     *
     * @param key   the registry key (e.g. "myaddon:custom_event")
     * @param clazz the trigger implementation class
     */
    public void registerTrigger(String key, Class<? extends SkillTrigger> clazz) {
        triggerRegistry.register(key, clazz);
    }

    /**
     * Registers an evaluator class under the given key.
     * The evaluator must have a public no-arg constructor.
     *
     * @param key   the registry key (e.g. "logistic")
     * @param clazz the evaluator implementation class
     */
    public void registerEvaluator(String key, Class<? extends ParameterEvaluator> clazz) {
        evaluatorRegistry.register(key, clazz);
    }

    /**
     * Registers an evaluator instance under the given key.
     *
     * @param key       the registry key (e.g. "logistic")
     * @param evaluator the evaluator instance
     * @deprecated Use {@link #registerEvaluator(String, Class)} instead.
     */
    @Deprecated
    public void registerEvaluator(String key, Object evaluator) {
        evaluatorRegistry.register(key, evaluator);
    }

    /**
     * Returns the mechanic registry.
     *
     * @return the mechanic registry
     */
    public MechanicRegistry getMechanicRegistry() {
        return mechanicRegistry;
    }

    /**
     * Returns the trigger registry.
     *
     * @return the trigger registry
     */
    public TriggerRegistry getTriggerRegistry() {
        return triggerRegistry;
    }

    /**
     * Returns the evaluator registry.
     *
     * @return the evaluator registry
     */
    public EvaluatorRegistry getEvaluatorRegistry() {
        return evaluatorRegistry;
    }
}