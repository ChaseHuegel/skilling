# `getLevelForXp()` O(maxLevel) linear scan duplicated in `SkillEventListener` and `LevelUpDispatcher`

## Issue

The method `getLevelForXp(SkillDefinition, long xp)` is implemented identically in two places:

- `SkillEventListener.java` (lines 533-539)
- `LevelUpDispatcher.java` (lines 247-254)

Both iterate from level 1 to `maxLevel`, summing XP requirements via `skill.progression().evaluator().evaluate(level, 0)` until the cumulative XP exceeds the player's total. This is an O(maxLevel) linear scan with duplicated logic.

**ISSUES.md reference:** Line 310

## Root Cause

The method was copy-pasted to avoid cross-package dependencies instead of being centralized on `SkillDefinition`.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `SkillEventListener.java` | `engine/listener/SkillEventListener.java` | 533-539 |
| `LevelUpDispatcher.java` | `engine/feedback/LevelUpDispatcher.java` | 247-254 |
| `SkillMenuBuilder.java` | `engine/ui/SkillMenuBuilder.java` | 165 (also has its own copy) |
| `SkillsCommand.java` | `engine/command/SkillsCommand.java` | 437-438 (delegates to LevelUpDispatcher) |

## Development Plan

### Step 1: Centralize on `SkillDefinition`

Add a non-static method to `SkillDefinition`:

```java
public int getLevelForXp(long xp) {
    for (int level = 1; level <= maxLevel; level++) {
        double required = progression.evaluator().evaluate(level, 0);
        if (xp < (long) required) return level - 1;
    }
    return maxLevel;
}
```

### Step 2: Replace all usages

Replace the 3 duplicate methods with `skill.getLevelForXp(xp)`:

- `SkillEventListener.java:533` → `skill.getLevelForXp(xp)`
- `LevelUpDispatcher.java:247` → `skill.getLevelForXp(xp)`
- `SkillMenuBuilder.java:165` → `skill.getLevelForXp(xp)`
- `SkillsCommand.java:437-438` already delegates to `LevelUpDispatcher`, so it benefits transitively once centralized

### Step 3: Remove now-unused private methods

Delete `SkillEventListener.getLevelForXp()`, `LevelUpDispatcher.getLevelForXp()`, and `SkillMenuBuilder.getLevelForXp()`.

### Step 4: Verify

- `./gradlew build` should pass
- All level lookups (menu, bossbar, level-up detection, commands) should return identical results

## Self-Review

- Eliminates 3 copies of the same O(maxLevel) loop
- The method logically belongs on `SkillDefinition` (which already holds `maxLevel` and `progression`)
- No behavior change — same logic, same performance
- Simplifies maintenance — XP curve changes only need updates in one place
