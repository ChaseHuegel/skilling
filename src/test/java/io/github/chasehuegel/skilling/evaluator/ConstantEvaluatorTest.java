package io.github.chasehuegel.skilling.evaluator;

import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ConstantEvaluatorTest {

    @Test
    void returnsFixedValue() {
        var eval = new ConstantEvaluator(42.0);
        assertEquals(42.0, eval.evaluate(0, 0), 1e-9);
        assertEquals(42.0, eval.evaluate(100, 0), 1e-9);
        assertEquals(42.0, eval.evaluate(50, 50), 1e-9);
    }

    @Test
    void zeroValue() {
        var eval = new ConstantEvaluator(0.0);
        assertEquals(0.0, eval.evaluate(0, 0), 1e-9);
        assertEquals(0.0, eval.evaluate(100, 0), 1e-9);
    }

    @Test
    void negativeValue() {
        var eval = new ConstantEvaluator(-10.0);
        assertEquals(-10.0, eval.evaluate(0, 0), 1e-9);
    }
}