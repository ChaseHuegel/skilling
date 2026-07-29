# 6 trigger implementations use fully-qualified class names instead of imports

## Issue

Six trigger classes reference their event class via fully-qualified name in `getEventClass()` instead of using a proper import. Example from `RideHorseTrigger.java`:

```java
@Override
public Class<? extends Event> getEventClass() { return org.bukkit.event.player.PlayerInteractEntityEvent.class; }
```

This is inconsistent with the other 13 triggers that use imports. The affected triggers:
- `RideHorseTrigger.java` — `PlayerInteractEntityEvent`
- `BlockBreakTrigger.java`, `BlockPlaceTrigger.java` — likely similar pattern
- Three more triggers

**ISSUES.md reference:** Line 305

## Root Cause

These triggers were implemented without adding the import statement, likely following an earlier convention or oversight.

## Affected Files

| File | Path |
|------|------|
| Various trigger implementations | `engine/trigger/impl/*.java` |

## Development Plan

### Step 1: Identify all 6 triggers

Search for `.class;` after a fully-qualified class name in the return statement of `getEventClass()` across all trigger implementations.

### Step 2: Fix each one

For each affected trigger:
1. Add the appropriate `import` statement
2. Replace the fully-qualified name with the simple class name

### Step 3: Verify

- `./gradlew build` should pass
- No functional change — fully-qualified names are semantically identical to imported names

## Self-Review

- Purely cosmetic/style fix — no behavior change
- Improves readability and consistency
- The trigger list is small enough to fix all at once
