package io.github.chasehuegel.skilling.engine.evaluator.impl;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;

/**
 * Evaluator that returns a fixed value regardless of level.
 *
 * <p>YAML key: {@code constant}
 * <br>Parameters: {@code value} (double or string) — the constant output value.
 *
 * <p>A numeric constant produces a {@code double} from {@link #evaluate}; a
 * string constant (e.g. a namespaced effect key) cannot be represented as a
 * {@code double}, so it is carried verbatim and surfaced via {@link #rawValue()}
 * so parameter evaluation and load-time validation can emit it unchanged without
 * type-special-casing a second evaluator class.
 *
 * <p>Formula: {@code result = value}
 */
public record ConstantEvaluator(double value, String stringValue) implements ParameterEvaluator {

    /**
     * Creates a numeric constant.
     *
     * @param value the constant output value
     */
    public ConstantEvaluator(double value) {
        this(value, null);
    }

    /**
     * Creates a string constant (e.g. a namespaced effect, attribute, or material
     * key). {@link #evaluate} returns {@code 0.0} for such a constant; the raw
     * value is read via {@link #rawValue()}.
     *
     * @param stringValue the raw constant output
     */
    public ConstantEvaluator(String stringValue) {
        this(0.0, stringValue);
    }

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        return value;
    }

    /**
     * Returns the constant output as its raw type: the numeric value for a
     * numeric constant, or the verbatim string for a string constant.
     *
     * @return the constant's raw output value
     */
    public Object rawValue() {
        return stringValue != null ? stringValue : value;
    }
}
