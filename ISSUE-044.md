# `ModifyAttributeMechanic`, `ArmorBonusMechanic`, `SpeedBonusMechanic`, `KnockbackResistMechanic` use hardcoded `JavaPlugin.getPlugin()` lookup

## Issue

Four mechanics use `JavaPlugin.getPlugin(Skilling.class)` to obtain the plugin instance instead of using the injected `Skilling.getInstance()` pattern or having the plugin instance passed in. This creates a hidden dependency and makes unit testing harder (the lookup fails without a running server).

Affected lines:
- `ModifyAttributeMechanic.java`: lines 47-49
- `ArmorBonusMechanic.java`: lines 30-31
- `SpeedBonusMechanic.java`: lines 31-32
- `KnockbackResistMechanic.java`: lines 28-29

**ISSUES.md reference:** Line 299

## Root Cause

These mechanics schedule delayed tasks using `player.getScheduler().runDelayed(plugin, ...)`. Instead of accepting `Skilling` via constructor injection, they hardcode the lookup. This is inconsistent with the project's patterns and breaks testability.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `ModifyAttributeMechanic.java` | `engine/mechanic/impl/ModifyAttributeMechanic.java` | 47-49 |
| `ArmorBonusMechanic.java` | `engine/mechanic/impl/ArmorBonusMechanic.java` | 30-31 |
| `SpeedBonusMechanic.java` | `engine/mechanic/impl/SpeedBonusMechanic.java` | 31-32 |
| `KnockbackResistMechanic.java` | `engine/mechanic/impl/KnockbackResistMechanic.java` | 28-29 |

## Development Plan

### Step 1: Replace with `Skilling.getInstance()`

Replace `JavaPlugin.getPlugin(Skilling.class)` with `Skilling.getInstance()` in all four files. This is a singleton accessor already defined in `Skilling.java`.

```java
// Before:
org.bukkit.plugin.java.JavaPlugin.getPlugin(io.github.chasehuegel.skilling.Skilling.class)
// After:
Skilling.getInstance()
```

### Step 2: Add necessary imports

Ensure all four files import `io.github.chasehuegel.skilling.Skilling`.

### Step 3: Remove unused import of `JavaPlugin` if no longer needed

## Self-Review

- Simple substitution — `Skilling.getInstance()` returns the same instance
- Reduces import complexity (removes `org.bukkit.plugin.java.JavaPlugin`)
- Consistent with the rest of the codebase
- `Skilling.getInstance()` is safe to call from any thread after `onEnable()` completes
