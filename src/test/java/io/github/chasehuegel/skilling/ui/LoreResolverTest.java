package io.github.chasehuegel.skilling.ui;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.ui.LoreResolver;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoreResolverTest {

    @Test
    void replacesPlaceholderWithEvaluatorOutput() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "yield_chance", new ConstantEvaluator(42.0)
        );
        String result = LoreResolver.resolve("Increases yield by {yield_chance}%.", evaluators, 50, 15);
        assertEquals("Increases yield by 42%.", result);
    }

    @Test
    void missingPlaceholderLeftUntouched() {
        Map<String, ParameterEvaluator> evaluators = Map.of();
        String result = LoreResolver.resolve("Value: {unknown}.", evaluators, 50, 15);
        assertEquals("Value: {unknown}.", result);
    }

    @Test
    void unknownPlaceholderKeepsTokenAndOtherPlaceholdersResolve() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "known", new ConstantEvaluator(7.0)
        );
        String result = LoreResolver.resolve("Known: {known}, unknown: {bogus}.", evaluators, 50, 15);
        assertEquals("Known: 7, unknown: {bogus}.", result);
    }

    @Test
    void linearEvaluatorOutput() {
        var linear = new LinearEvaluator(10.0, 2.0, 0.0, 100.0);
        Map<String, ParameterEvaluator> evaluators = Map.of("damage", linear);
        String result = LoreResolver.resolve("Damage: {damage}.", evaluators, 20, 15);
        assertEquals("Damage: 20.", result); // 10 + 2 * (20-15) = 20
    }

    @Test
    void integerOutputForWholeValues() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "count", new ConstantEvaluator(3.0)
        );
        String result = LoreResolver.resolve("Count: {count}.", evaluators, 50, 1);
        assertEquals("Count: 3.", result);
    }

    @Test
    void decimalOutputForFractionalValues() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "chance", new ConstantEvaluator(12.5)
        );
        String result = LoreResolver.resolve("Chance: {chance}%", evaluators, 50, 1);
        assertEquals("Chance: 12.5%", result);
    }

    @Test
    void multiplePlaceholders() {
        var linear = new LinearEvaluator(2.0, 0.5, 0.0, 10.0);
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "chain", new ConstantEvaluator(5.0),
                "cost", linear
        );
        String result = LoreResolver.resolve("Chain: {chain}, Cost: {cost}.", evaluators, 20, 15);
        assertEquals("Chain: 5, Cost: 4.5.", result); // 2.0 + 0.5 * 5 = 4.5
    }

    @Test
    void stringEvaluatorPlaceholderResolvesVerbatim() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "skill_name", new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantValueEvaluator("Mining")
        );
        String result = LoreResolver.resolve("Welcome to {skill_name}!", evaluators, 50, 15);
        assertEquals("Welcome to Mining!", result);
    }

    @Test
    void resolveAllLines() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "val", new ConstantEvaluator(7.0)
        );
        List<String> lore = List.of("Line 1: {val}", "Line 2", "Line 3: {val}%");
        List<String> resolved = LoreResolver.resolveAll(lore, evaluators, 50, 1);
        assertEquals(List.of("Line 1: 7", "Line 2", "Line 3: 7%"), resolved);
    }

    @Test
    void nullOrBlankLine() {
        Map<String, ParameterEvaluator> evaluators = Map.of(
                "val", new ConstantEvaluator(5.0)
        );
        assertNull(LoreResolver.resolve(null, evaluators, 50, 1));
        assertEquals("", LoreResolver.resolve("", evaluators, 50, 1));
        assertEquals("   ", LoreResolver.resolve("   ", evaluators, 50, 1));
    }
}