# `TeleportMechanic.findSafeLocation()` mutates `Location` in-place with chained `add()`/`subtract()` calls

## Issue

`TeleportMechanic.findSafeLocation()` (lines 42-55) uses chained `Location` mutation methods (`add()` and `subtract()`) that modify the location in-place and return the same instance. This is fragile and confusing:

```java
private Location findSafeLocation(Location loc) {
    Location safe = loc.clone();
    safe.setY(safe.getY() + 1);
    if (safe.getBlock().isEmpty() && safe.add(0, 1, 0).getBlock().isEmpty()) {
        return safe.subtract(0, 1, 0);
    }
    for (int y = 0; y > -3; y--) {
        Location check = loc.clone().add(0, y, 0);
        if (check.getBlock().isEmpty() && check.add(0, 1, 0).getBlock().isEmpty()) {
            return check;
        }
    }
    return null;
}
```

The chained `add(0, 1, 0)` mutates `safe` (or `check`) in-place, then `subtract(0, 1, 0)` reverses it — but the return value from `add()` is the same mutated instance, and `add()` is called for its side effect (to check the block above), then `subtract()` reverts to the original y-level. This is correct but extremely fragile — any reordering could break it.

**ISSUES.md reference:** Line 132

## Root Cause

`Location.add()` and `Location.subtract()` modify the instance in-place and return `this`. The code chains them for the side effects, creating a hard-to-read and error-prone pattern.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `TeleportMechanic.java` | `engine/mechanic/impl/TeleportMechanic.java` | 42-55 |

## Development Plan

### Step 1: Refactor to explicit variable usage

Replace chained mutation with explicit variable assignments:

```java
private Location findSafeLocation(Location loc) {
    Location safe = loc.clone();
    safe.setY(safe.getY() + 1);
    Location above = safe.clone().add(0, 1, 0);
    if (safe.getBlock().isEmpty() && above.getBlock().isEmpty()) {
        return safe;
    }
    for (int y = 0; y > -3; y--) {
        Location check = loc.clone().add(0, y, 0);
        Location aboveCheck = check.clone().add(0, 1, 0);
        if (check.getBlock().isEmpty() && aboveCheck.getBlock().isEmpty()) {
            return check;
        }
    }
    return null;
}
```

### Step 2: Verify

- Teleport to various locations (above ground, in wall, underground)
- `findSafeLocation` should find a safe spot with 2 blocks of headroom
- No in-place mutation of input `loc`

## Self-Review

- The original code works but is fragile — the refactor preserves behavior
- No `add()`/`subtract()` mutation side effects — all clones are explicit
- Easier to read and maintain
