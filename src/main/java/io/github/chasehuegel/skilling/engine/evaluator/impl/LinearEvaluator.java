package io.github.chasehuegel.skilling.engine.evaluator.impl;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;

/**
 * Evaluator that scales linearly with level above the unlock point,
 * clamped to an optional {@code min} and {@code max}.
 *
 * <p>YAML key: {@code linear}
 * <br>Parameters:
 * <ul>
 *   <li>{@code base} (double) — value at {@code unlockLevel}</li>
 *   <li>{@code step} (double) — added per level above unlock</li>
 *   <li>{@code max} (double, optional) — hard upper clamp</li>
 *   <li>{@code min} (double, optional) — hard lower clamp</li>
 * </ul>
 *
 * <p>Formula: {@code result = clamp(base + step * (currentLevel - unlockLevel))}
 */
public final class LinearEvaluator implements ParameterEvaluator {

    private final double base;
    private final double step;
    private final double min;
    private final double max;

    /**
     * Constructs a linear evaluator.
     *
     * @param base value at unlockLevel
     * @param step added per level above unlock
     * @param min  minimum clamp (use -Infinity for no floor)
     * @param max  maximum clamp (use +Infinity for no ceiling)
     */
    public LinearEvaluator(double base, double step, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException(
                    "linear evaluator min (" + min + ") must be <= max (" + max + ")");
        }
        this.base = base;
        this.step = step;
        this.min = min;
        this.max = max;
    }

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        double raw = base + step * (currentLevel - unlockLevel);
        return Math.clamp(raw, min, max);
    }
}