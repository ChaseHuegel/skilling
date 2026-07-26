package io.github.chasehuegel.skilling.engine.evaluator;

/**
 * A stateless math processor that calculates a dynamic value based on a
 * player's current level relative to an ability's unlock level.
 *
 * <p>Implementations are registered in {@link io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry}
 * under kebab-case keys (e.g., {@code linear}, {@code milestone}) and referenced
 * from YAML parameter blocks.
 */
@FunctionalInterface
public interface ParameterEvaluator {

    /**
     * Evaluates the output value for the given level parameters.
     *
     * @param currentLevel the player's current level in the relevant skill
     * @param unlockLevel  the level at which the ability is unlocked
     * @return the computed value
     */
    double evaluate(int currentLevel, int unlockLevel);
}