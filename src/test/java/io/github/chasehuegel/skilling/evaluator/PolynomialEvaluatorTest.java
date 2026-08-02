package io.github.chasehuegel.skilling.evaluator;

import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PolynomialEvaluatorTest {

    @Test
    void levelOneReturnsBaseXp() {
        var eval = new PolynomialEvaluator(50.0, 2.5);
        assertEquals(50.0, eval.evaluate(1, 0), 1e-9);
    }

    @Test
    void levelZeroReturnsZero() {
        var eval = new PolynomialEvaluator(50.0, 2.5);
        assertEquals(0.0, eval.evaluate(0, 0), 1e-9);
    }

    @Test
    void computesCorrectTotalXp() {
        var eval = new PolynomialEvaluator(50.0, 2.0);
        // 50 * 1^2 = 50
        assertEquals(50.0, eval.xpForLevel(1), 1e-9);
        // 50 * 2^2 = 200
        assertEquals(200.0, eval.xpForLevel(2), 1e-9);
        // 50 * 10^2 = 5000
        assertEquals(5000.0, eval.xpForLevel(10), 1e-9);
    }

    @Test
    void evaluateUsesCurrentLevelOnly() {
        var eval = new PolynomialEvaluator(50.0, 2.0);
        // evaluate() ignores unlockLevel for polynomial
        assertEquals(50.0, eval.evaluate(1, 10), 1e-9);
        assertEquals(200.0, eval.evaluate(2, 5), 1e-9);
    }

    @Test
    void highLevelScaling() {
        var eval = new PolynomialEvaluator(1.0, 3.0);
        // 1 * 100^3 = 1_000_000
        assertEquals(1_000_000.0, eval.evaluate(100, 0), 1e-9);
    }

    @Test
    void negativeBaseThrows() {
        assertThrows(IllegalArgumentException.class, () -> new PolynomialEvaluator(-50.0, 2.5));
    }

    @Test
    void zeroOrNegativeExponentThrows() {
        assertThrows(IllegalArgumentException.class, () -> new PolynomialEvaluator(50.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PolynomialEvaluator(50.0, -1.0));
    }

    @Test
    void nonFiniteParamsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new PolynomialEvaluator(Double.NaN, 2.5));
        assertThrows(IllegalArgumentException.class, () -> new PolynomialEvaluator(50.0, Double.POSITIVE_INFINITY));
    }
}