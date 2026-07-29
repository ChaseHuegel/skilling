# `MilestoneEvaluator` accepts `TreeMap` without defensive copy — mutable input stored directly

## Issue

`MilestoneEvaluator`'s constructor (line 25) stores the passed `TreeMap` reference directly:

```java
public MilestoneEvaluator(TreeMap<Integer, Double> milestones) {
    this.milestones = milestones;
}
```

The caller in `SkillManager.parseInlineEvaluator()` (line 356) creates the `TreeMap` locally and passes it directly:
```java
return new MilestoneEvaluator(milestones);
```

Since `MilestoneEvaluator` is registered as a singleton in `Skilling.registerBuiltins()` (line 205):
```java
evalReg.register("milestone", new MilestoneEvaluator(new TreeMap<>()));
```

The singleton's `milestones` map can be mutated externally if someone retains a reference to the original `TreeMap`. While the YAML parsing path creates a new map each time (so skill instances are safe), the singleton evaluator used as a registry prototype could have its internal state corrupted.

**ISSUES.md reference:** Line 303

## Root Cause

The constructor does not defensively copy the input map, violating encapsulation.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `MilestoneEvaluator.java` | `engine/evaluator/impl/MilestoneEvaluator.java` | 25-27 |
| `SkillManager.java` | `engine/SkillManager.java` | 356 |
| `Skilling.java` | `Skilling.java` | 205 |

## Development Plan

### Step 1: Add defensive copy

```java
public MilestoneEvaluator(TreeMap<Integer, Double> milestones) {
    this.milestones = new TreeMap<>(milestones);
}
```

### Step 2: Verify

- Existing `MilestoneEvaluator` tests, if any, should still pass
- Parsing milestones from YAML should work as before
- The singleton registry entry should not be affected since it receives an empty map

## Self-Review

- One-line change — `new TreeMap<>(milestones)` performs a shallow copy
- `TreeMap` keys (`Integer`) and values (`Double`) are immutable, so shallow copy is sufficient
- No performance concern — `TreeMap` copy is O(n) where n is typically < 50 entries
- Prevents external corruption of the evaluator's internal state
