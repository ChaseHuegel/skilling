package io.github.chasehuegel.skilling.engine.evaluator.impl;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;

/**
 * Evaluator that returns a fixed value regardless of level.
 *
 * <p>YAML key: {@code constant}
 * <br>Parameters: {@code value} (double) — the constant output value.
 *
 * <p>Formula: {@code result = value}
 */
public record ConstantEvaluator(double value) implements ParameterEvaluator {

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        return value;
    }
}