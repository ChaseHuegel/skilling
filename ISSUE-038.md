# `RequirementEngine` state checks use `default -> true` — unknown states silently pass

## Issue

In `RequirementEngine.checkState()` (lines 116-124), the switch expression has a `default -> true` branch:

```java
private boolean checkState(Player player, String state) {
    return switch (state) {
        case "is_sneaking" -> player.isSneaking();
        case "is_sprinting" -> player.isSprinting();
        case "is_in_water" -> player.isInWater();
        case "is_on_ground" -> player.isOnGround();
        default -> true; // unknown states pass through
    };
}
```

Unknown or misspelled state names silently pass the check, making it impossible to detect configuration errors. For example, `is_sneekng` (misspelled) or `is_flying` (unsupported) would always pass, and the skill designer would never know the state was ignored.

**ISSUES.md reference:** Line 133

## Root Cause

The `default -> true` branch was implemented as a "fail-open" pattern but this hides configuration mistakes.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `RequirementEngine.java` | `engine/requirements/RequirementEngine.java` | 116-124 |

## Development Plan

### Step 1: Change `default` to log warning and return `false`

```java
default -> {
    Skilling.getInstance().getLogger().warning(
        "Unknown state check: '" + state + "' — failing requirement"
    );
    yield false;
}
```

Or, to fail-fast during parsing rather than at runtime, validate state strings in `SkillManager.parseRequirements()`.

### Step 2: (Optional) Fail-fast during YAML parsing

In `SkillManager.parseRequirements()` or `parseAbilityDisplay()`, validate state strings against a known set and throw `IllegalArgumentException` for unknown values. This catches errors at plugin load/reload time.

### Step 3: Verify

- A skill with `state: ["is_flying"]` should fail the check
- The warning should appear in the server log
- Known states (`is_sneaking`, `is_sprinting`, `is_in_water`, `is_on_ground`) should continue to work

## Self-Review

- The `default -> false` approach is the minimal fix — unknown states block ability activation
- The fail-fast option is preferred for catching config errors early
- Both approaches are backward-incompatible for anyone using unsupported states; a warning log is less disruptive than an exception on reload
