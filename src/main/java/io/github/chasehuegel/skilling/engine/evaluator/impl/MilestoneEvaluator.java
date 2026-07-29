package io.github.chasehuegel.skilling.engine.evaluator.impl;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import java.util.TreeMap;

/**
 * Evaluator that returns the value associated with the highest milestone
 * level that is less than or equal to the current level.
 *
 * <p>YAML key: {@code milestones}
 * <br>Parameters: a map of {@code level -> value} entries.
 *
 * <p>Uses a {@link TreeMap#floorKey(Object)} lookup for O(log n) performance.
 * Returns 0 if no milestone is applicable.
 */
public final class MilestoneEvaluator implements ParameterEvaluator {

    private final TreeMap<Integer, Double> milestones;

    /**
     * Constructs a milestone evaluator.
     *
     * @param milestones an ordered map of level → value thresholds
     */
    public MilestoneEvaluator(TreeMap<Integer, Double> milestones) {
        this.milestones = new TreeMap<>(milestones);
    }

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        var key = milestones.floorKey(currentLevel);
        if (key == null) {
            return 0.0;
        }
        return milestones.get(key);
    }
}