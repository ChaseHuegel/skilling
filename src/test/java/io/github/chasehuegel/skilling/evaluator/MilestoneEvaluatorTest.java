package io.github.chasehuegel.skilling.evaluator;

import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.TreeMap;

class MilestoneEvaluatorTest {

    @Test
    void returnsValueForExactLevel() {
        var milestones = new TreeMap<Integer, Double>();
        milestones.put(15, 3.0);
        milestones.put(40, 8.0);
        milestones.put(80, 16.0);

        var eval = new MilestoneEvaluator(milestones);
        assertEquals(3.0, eval.evaluate(15, 1), 1e-9);
        assertEquals(8.0, eval.evaluate(40, 1), 1e-9);
        assertEquals(16.0, eval.evaluate(80, 1), 1e-9);
    }

    @Test
    void floorKeyBetweenMilestones() {
        var milestones = new TreeMap<Integer, Double>();
        milestones.put(15, 3.0);
        milestones.put(40, 8.0);

        var eval = new MilestoneEvaluator(milestones);
        assertEquals(3.0, eval.evaluate(20, 1), 1e-9);
        assertEquals(3.0, eval.evaluate(39, 1), 1e-9);
        assertEquals(8.0, eval.evaluate(60, 1), 1e-9);
    }

    @Test
    void belowFirstMilestoneReturnsZero() {
        var milestones = new TreeMap<Integer, Double>();
        milestones.put(15, 3.0);

        var eval = new MilestoneEvaluator(milestones);
        assertEquals(0.0, eval.evaluate(1, 1), 1e-9);
        assertEquals(0.0, eval.evaluate(14, 1), 1e-9);
    }

    @Test
    void aboveHighestMilestone() {
        var milestones = new TreeMap<Integer, Double>();
        milestones.put(15, 3.0);

        var eval = new MilestoneEvaluator(milestones);
        assertEquals(3.0, eval.evaluate(100, 1), 1e-9);
    }

    @Test
    void emptyMilestones() {
        var eval = new MilestoneEvaluator(new TreeMap<>());
        assertEquals(0.0, eval.evaluate(50, 1), 1e-9);
    }
}