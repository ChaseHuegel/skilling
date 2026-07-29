# `ArmorBonusMechanic`, `SpeedBonusMechanic`, `KnockbackResistMechanic` hardcode 6000-tick (5 minute) duration with no configurable `duration` param

## Issue

Three attribute mechanics hardcode a 6000-tick (5 minute) duration for the transient attribute modifier:

- `ArmorBonusMechanic.java`: line 31 (`6000L`)
- `SpeedBonusMechanic.java`: line 32 (`6000L`)
- `KnockbackResistMechanic.java`: line 29 (`6000L`)

They should accept an optional `duration` parameter (like `ModifyAttributeMechanic` does). Additionally, `ArmorBonusMechanic`'s Javadoc says "Permanently increases" but the effect is temporary — the documentation is wrong.

**ISSUES.md reference:** Lines 300-301

## Root Cause

The mechanics were implemented with a hardcoded timeout and no parameter support for custom duration.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `ArmorBonusMechanic.java` | `engine/mechanic/impl/ArmorBonusMechanic.java` | 13, 31 |
| `SpeedBonusMechanic.java` | `engine/mechanic/impl/SpeedBonusMechanic.java` | 30-32 |
| `KnockbackResistMechanic.java` | `engine/mechanic/impl/KnockbackResistMechanic.java` | 27-29 |

## Development Plan

### Step 1: Add `duration` parameter support

For each mechanic, read an optional `duration` parameter (default 300, i.e., 5 minutes in seconds to match `ModifyAttributeMechanic`'s convention):

```java
int duration = ((Number) params.getOrDefault("duration", 300.0)).intValue();
// ...
null, duration * 20L
```

### Step 2: Update Javadoc for `ArmorBonusMechanic`

Change "Permanently increases" to "Temporarily increases for the specified duration." and document the `duration` parameter.

### Step 3: Update `docs/capabilities.md`

Add `duration` (optional, default 300s) to the parameter tables for all three mechanics.

## Self-Review

- Adds flexibility while maintaining backward compatibility (default 300s ≈ 5 min)
- `ModifyAttributeMechanic` already uses this exact pattern — no innovation needed
- The `6000L` → `duration * 20L` conversion is consistent (seconds → ticks)
- Javadoc fix prevents user confusion about permanent vs. temporary effects
