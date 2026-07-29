# `ApplyStatusMechanic` does not verify `getDamager().equals(player)` — could fire when player is the damage receiver

## Issue

`ApplyStatusMechanic` checks `event instanceof EntityDamageByEntityEvent` and applies the potion effect to `event.getEntity()` (the damage target), but never verifies that `event.getDamager().equals(player)` — i.e., that the player is the attacker, not the receiver. If another entity damages the player while this mechanic is active, the effect would be incorrectly applied to the player instead of to the entity they're attacking.

**ISSUES.md reference:** Line 130

## Root Cause

The execute method (lines 22-34) checks only:
```java
if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
if (!(damageEvent.getEntity() instanceof LivingEntity target)) return false;
```

It never verifies the player is the attacker. Compare with `DodgeMechanic`/`BlockDamageMechanic` which check `de.getEntity().equals(player)`.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `ApplyStatusMechanic.java` | `engine/mechanic/impl/ApplyStatusMechanic.java` | 23-24 |

## Development Plan

### Step 1: Add damager check

```java
if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
if (!damageEvent.getDamager().equals(player)) return false;
if (!(damageEvent.getEntity() instanceof LivingEntity target)) return false;
```

### Step 2: Verify

- Player attacks entity with `apply_status` mechanic → effect applied to entity ✓
- Entity attacks player → mechanic returns false, no effect applied ✓

## Self-Review

- One-line addition, consistent with other mechanics
- Corrects behavioral bug that could apply debuffs to the player when hit
- No performance impact
