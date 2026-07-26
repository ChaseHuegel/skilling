package io.github.chasehuegel.skilling.evaluator;

import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LinearEvaluatorTest {

    @Test
    void baseValueAtUnlockLevel() {
        var eval = new LinearEvaluator(10.0, 2.0, 0.0, 100.0);
        assertEquals(10.0, eval.evaluate(15, 15), 1e-9);
    }

    @Test
    void scalesLinearly() {
        var eval = new LinearEvaluator(10.0, 2.0, 0.0, 100.0);
        // At level 15 (unlock): 10 + 2 * (15-15) = 10
        // At level 20 (unlock 15): 10 + 2 * (20-15) = 20
        assertEquals(20.0, eval.evaluate(20, 15), 1e-9);
        assertEquals(30.0, eval.evaluate(25, 15), 1e-9);
    }

    @Test
    void clampedToMax() {
        var eval = new LinearEvaluator(10.0, 2.0, 0.0, 25.0);
        assertEquals(25.0, eval.evaluate(100, 15), 1e-9);
    }

    @Test
    void clampedToMin() {
        var eval = new LinearEvaluator(10.0, -2.0, 1.0, 100.0);
        // level below unlock: 10 + (-2) * (10-15) = 10 + 10 = 20, but with negative step going down
        // unlock=15, level=5: 10 + -2 * (5-15) = 10 + 20 = 30
        assertEquals(1.0, eval.evaluate(50, 15), 1e-9); // 10 + -2 * 35 = -60, clamped to 1
    }

    @Test
    void noClampWithInfinity() {
        var eval = new LinearEvaluator(0.0, 5.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(500.0, eval.evaluate(100, 0), 1e-9);
    }

    @Test
    void levelZero() {
        var eval = new LinearEvaluator(5.0, 1.0, 0.0, 100.0);
        assertEquals(5.0, eval.evaluate(0, 0), 1e-9);
        assertEquals(0.0, eval.evaluate(0, 5), 1e-9);
    }
}