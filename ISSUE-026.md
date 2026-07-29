# `CancelDamageMechanic` return value and entity check inconsistency with `DodgeMechanic`/`BlockDamageMechanic`

## Issue

`CancelDamageMechanic` has two inconsistencies compared to the functionally identical `DodgeMechanic` and `BlockDamageMechanic`:

1. **Return value on failed roll**: `CancelDamageMechanic` returns `true` even when the damage roll fails (line 26). `DodgeMechanic` and `BlockDamageMechanic` correctly return `false` when the roll fails (line 27 of each), indicating the mechanic did not activate. This causes the ability pipeline to report a successful activation even when no damage was actually cancelled.

2. **Missing entity check**: `CancelDamageMechanic` does not verify `getEntity().equals(player)` before cancelling damage. `DodgeMechanic` (line 20) and `BlockDamageMechanic` (line 20) both check this. Without it, `CancelDamageMechanic` could cancel damage to any entity, not just the player who activated the ability.

**ISSUES.md reference:** Lines 119-121

## Root Cause

`CancelDamageMechanic` was implemented first and doesn't follow the same pattern established by the later `DodgeMechanic` and `BlockDamageMechanic` implementations.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `CancelDamageMechanic.java` | `engine/mechanic/impl/CancelDamageMechanic.java` | 23-26 |

## Development Plan

### Step 1: Add entity check

Add `if (!de.getEntity().equals(player)) return false;` after the event type check, matching `DodgeMechanic` and `BlockDamageMechanic`.

### Step 2: Fix return value

Change from always returning `true` to returning `true` only on successful roll, `false` otherwise:

```java
if (ThreadLocalRandom.current().nextDouble(100) < chance) {
    damageEvent.setCancelled(true);
    return true;
}
return false;
```

### Step 3: Verify consistency

After the change, all three damage-cancelling mechanics should follow the exact same pattern:
1. Check event type → return false
2. Check entity equals player → return false
3. Check chance ≤ 0 → return false
4. Roll random → if success, cancel + return true
5. Return false

## Self-Review

- Inline with `DodgeMechanic` and `BlockDamageMechanic` — establishes a convention
- Fixing the return value ensures the ability pipeline correctly reports activation status
- Fixing the entity check prevents exploit (cancelling damage to non-player entities)
