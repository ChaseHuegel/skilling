# `ModifyBrewTimeMechanic` hooks `BrewEvent` — modifies next batch, not current

## Issue

`ModifyBrewTimeMechanic` hooks `BrewEvent` (which fires when brewing **finishes**) and modifies the brewing stand's remaining time. By the time `BrewEvent` fires, the current batch is already complete. Modifying `stand.setBrewingTime()` at this point affects the **next** batch, not the one that just finished.

The mechanic should hook `BrewEvent` before the brewing starts — or more accurately, the trigger should fire on `BrewingStandRecipeEvent` or `BrewEvent` with proper timing so the time multiplier applies to the batch being started.

**ISSUES.md reference:** Line 123

## Root Cause

`BrewEvent` is fired when the brewing stand finishes its cycle. The mechanic reads the current brewing time (which is near 0 at completion) and modifies it, but this affects the next fuel-triggered cycle, not the batch the player just completed.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `ModifyBrewTimeMechanic.java` | `engine/mechanic/impl/ModifyBrewTimeMechanic.java` | 19-31 |

## Development Plan

### Step 1: Change event hook

Replace `BrewEvent` with `BrewingStandRecipeEvent` (Paper API, fires when ingredients are placed and brewing starts). This event fires when a new brewing cycle begins, allowing the mechanic to set the brewing time before the cycle starts.

Alternatively, if `BrewingStandRecipeEvent` is not available or the server runs Spigot, use `BrewEvent` but set the brewing time on the NEXT cycle by listening for the fuel consumption or ingredient placement.

### Step 2: Update mechanic parameters

The `BrewingStandRecipeEvent` provides access to the new recipe and the stand. Set the brewing time directly on the stand:

```java
if (event instanceof BrewingStandRecipeEvent brewEvent) {
    if (brewEvent.getHolder() instanceof BrewingStand stand) {
        int baseTime = stand.getBrewingTime();  // default 400 ticks
        int newTime = (int) Math.round(baseTime * multiplier);
        stand.setBrewingTime(Math.max(1, newTime));
        return true;
    }
}
```

### Step 3: Update Javadoc

Update the class Javadoc to reflect the new event type and timing behavior.

## Self-Review

- `BrewingStandRecipeEvent` is a Paper API addition — check server compatibility
- If targeting Spigot/vanilla Bukkit, use a different approach: cancel the `BrewEvent`, prevent the potions from dispensing, re-add ingredients, and start a new cycle with modified time
- The Paper-only approach is simpler and aligns with the project's Paper API target
