# Three damage-cancelling mechanics have nearly identical logic — refactor into a shared base

## Issue

`CancelDamageMechanic`, `DodgeMechanic`, and `BlockDamageMechanic` all implement the same pattern: listen on `EntityDamageEvent`, check entity equals player, roll a random chance, cancel the event on success. The only differences are:
- `CancelDamageMechanic` is missing the entity check and has the wrong return value (covered in ISSUE-026)
- All three use slightly different random roll syntax

This is a prime candidate for a shared base class or utility method.

**ISSUES.md reference:** Line 298

## Root Cause

The mechanics were implemented independently without extracting the common pattern.

## Affected Files

| File | Path |
|------|------|
| `CancelDamageMechanic.java` | `engine/mechanic/impl/CancelDamageMechanic.java` |
| `DodgeMechanic.java` | `engine/mechanic/impl/DodgeMechanic.java` |
| `BlockDamageMechanic.java` | `engine/mechanic/impl/BlockDamageMechanic.java` |

## Development Plan

### Step 1: Create abstract base class

```java
public abstract class BaseDamageCancelMechanic implements SkillMechanic {
    protected abstract double getChance(Map<String, Object> params);

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double chance = getChance(params);
        if (chance <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            de.setCancelled(true);
            return true;
        }
        return false;
    }
}
```

### Step 2: Refactor each mechanic to extend the base

```java
// CancelDamageMechanic
public final class CancelDamageMechanic extends BaseDamageCancelMechanic {
    @Override
    protected double getChance(Map<String, Object> params) {
        return ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
    }
}

// DodgeMechanic — same pattern
// BlockDamageMechanic — same pattern
```

### Step 3: Move base class to appropriate package

Place in `engine/mechanic/` alongside `SkillMechanic`, named `BaseDamageCancelMechanic`.

### Step 4: Update docs/capabilities.md if needed

## Self-Review

- Eliminates ~40 lines of duplicated logic across 3 files
- Each mechanic becomes a 10-line class with just the `getChance()` override
- The entity check fix from ISSUE-026 is automatically applied to all three
- The `<=` fix from ISSUE-036 is applied uniformly
- Backward compatible — same YAML keys, same parameters
