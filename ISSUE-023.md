# XP reward evaluator hardcodes `(1, 1)` — rewards don't scale with player level

## Issue

In `SkillEventListener.grantXp()`, the XP reward is calculated as:

```java
double xp = source.reward().evaluate(1, 1) * plugin.getGlobalXpModifier();
```

The `evaluate(currentLevel, unlockLevel)` method always receives `(1, 1)`, so XP rewards never scale with the player's actual skill level. A `linear` evaluator configured with `base: 10, step: 5` would award 10 XP at level 1, 15 at level 2, etc. — but the hardcoded `1` for `currentLevel` means it always returns the level-1 amount (10 XP) regardless of progression.

**ISSUES.md reference:** Line 116

## Root Cause

`SkillEventListener.grantXp()` (line 298) does not compute the player's current skill level before evaluating the XP reward. The `evaluate` method signature is `double evaluate(int currentLevel, int unlockLevel)` — the first parameter is meant to be the player's current level in that skill, but it's hardcoded to `1`.

The player's current level IS computed a few lines later (line 301) for level-up detection, but it's not used in the reward calculation.

## Affected Files

| File | Path | Line | Role |
|------|------|------|------|
| `SkillEventListener.java` | `engine/listener/SkillEventListener.java` | 298 | Hardcoded `evaluate(1, 1)` |

## Development Plan

### Step 1: Compute player level before XP reward evaluation

In `grantXp()`, move the level computation before the reward evaluation:

```java
int oldLevel = getLevelForXp(skill, profile.getXp(skill.id()));
double xp = source.reward().evaluate(oldLevel, 1) * plugin.getGlobalXpModifier();
```

The `unlockLevel` parameter (second argument) is not applicable to XP sources (they don't have an unlock level), so passing `1` is acceptable. Alternatively, pass `oldLevel` for both to avoid any edge case where unlockLevel > currentLevel could produce negative results in `LinearEvaluator`.

### Step 2: Verify behavior with different evaluator types

| Evaluator | Before (always level 1) | After (actual level) |
|-----------|------------------------|----------------------|
| `constant: 15` | 15 at all levels | 15 at all levels ✓ |
| `linear: { base: 10, step: 5 }` | 10 at all levels | 10 at lvl 1, 15 at lvl 2, ... ✓ |
| `milestones: { 1: 10, 10: 50, 50: 200 }` | 10 at all levels | 10 at lvl 1-9, 50 at lvl 10-49, ... ✓ |

### Step 3: Verify `SkillsCommand.addXp()` / `setLevel()` flow

Check `SkillsCommand.java` to ensure it properly delegates to the same `grantXp`-equivalent logic and doesn't bypass the reward evaluator.

### Testing

- Create a skill with `reward: { linear: { base: 10, step: 5 } }`
- Grant XP at level 1: should receive 10 XP
- Grant XP at level 10: should receive `10 + 5 * (10 - 1) = 55` XP
- Verify `constant` evaluator yields unchanged behavior (same value at all levels)

## Self-Review

- Minimal change: one-line edit in `SkillEventListener.java:298`
- The `getLevelForXp()` call is computationally cheap (iterates maxLevel, typically ≤ 100)
- No regression for `constant` evaluators — they ignore both parameters entirely
- The `unlockLevel=1` is a safe default since XP sources have no concept of unlock levels; using `oldLevel` for both parameters would also work but is semantically misleading (the second parameter is documented as "the level at which the ability is unlocked")
- Backward compatible: all existing YAML configurations will produce higher XP at higher levels for non-constant evaluators, which is the intended behavior
