package io.github.chasehuegel.skilling.engine.evaluator.impl;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;

/**
 * Evaluator that computes XP requirements using a polynomial curve.
 *
 * <p>YAML key: {@code polynomial}
 * <br>Parameters:
 * <ul>
 *   <li>{@code base_xp} (double) — XP required for level 1</li>
 *   <li>{@code exponent} (double) — curve exponent</li>
 * </ul>
 *
 * <p>Formula: {@code requiredXp = baseXp * (level ^ exponent)}.
 * Used for the overall skill progression curve, not per-ability parameters.
 */
public record PolynomialEvaluator(double baseXp, double exponent) implements ParameterEvaluator {

    /**
     * Validates the curve parameters fail-fast at load: negative, zero, or
     * non-finite values would produce NaN/Infinity XP requirements at runtime.
     */
    public PolynomialEvaluator {
        if (!(baseXp > 0) || !Double.isFinite(baseXp)) {
            throw new IllegalArgumentException("polynomial base_xp must be a positive finite number, got: " + baseXp);
        }
        if (!(exponent > 0) || !Double.isFinite(exponent)) {
            throw new IllegalArgumentException("polynomial exponent must be a positive finite number, got: " + exponent);
        }
    }

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        // total XP required to reach currentLevel
        return baseXp * Math.pow(currentLevel, exponent);
    }

    /**
     * Computes the total XP required to reach a given level.
     *
     * @param level the target level
     * @return total XP required
     */
    public double xpForLevel(int level) {
        return baseXp * Math.pow(level, exponent);
    }
}