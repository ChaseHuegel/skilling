package io.github.chasehuegel.skilling.engine.evaluator.impl;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;

/**
 * Evaluator that carries a constant string value (e.g. a namespaced effect,
 * attribute, or material key) through parameter parsing.
 *
 * <p>YAML key: {@code constant}
 * <br>Parameters: {@code value} (string) — the raw constant output.
 *
 * <p>The {@link #evaluate(int, int)} contract returns a {@code double} and
 * cannot represent string output, so it returns {@code 0.0}. The raw value is
 * read via {@link #value()} by the parameter evaluation pipeline, which emits
 * it verbatim so string-typed mechanics receive the string unchanged.
 */
public record ConstantValueEvaluator(String value) implements ParameterEvaluator {

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        return 0.0;
    }
}
