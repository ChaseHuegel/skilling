# `AoeEffectMechanic`, `CrowdControlMechanic`, `ApplyStatusMechanic` use deprecated `PotionEffectType.getByName()`

## Issue

Three mechanics use `PotionEffectType.getByName()` (deprecated in Paper API) instead of `Registry.POTION_EFFECT_TYPE.get(NamespacedKey)`:

- `AoeEffectMechanic` — calls `getByName()` to resolve potion effect type
- `CrowdControlMechanic` — calls `getByName()` to resolve potion effect type
- `ApplyStatusMechanic` — line 28: `PotionEffectType.getByName(effectName.toUpperCase())`

**ISSUES.md reference:** Line 302

## Root Cause

The deprecated API was used during initial implementation. The modern Paper API uses `Registry.POTION_EFFECT_TYPE.get(NamespacedKey)` with lowercase namespaced keys (e.g., `minecraft:speed`).

## Affected Files

| File | Path |
|------|------|
| `ApplyStatusMechanic.java` | `engine/mechanic/impl/ApplyStatusMechanic.java` |
| `AoeEffectMechanic.java` | `engine/mechanic/impl/AoeEffectMechanic.java` |
| `CrowdControlMechanic.java` | `engine/mechanic/impl/CrowdControlMechanic.java` |

## Development Plan

### Step 1: Migrate to `Registry.POTION_EFFECT_TYPE`

Replace:
```java
PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
```

With:
```java
NamespacedKey effectKey = NamespacedKey.fromString(effectName.toLowerCase());
if (effectKey == null) return false;
PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(effectKey);
```

### Step 2: Update imports

Add `org.bukkit.Registry`, `org.bukkit.NamespacedKey`. Remove unused `PotionEffectType` import if it's still needed for `new PotionEffect(type, ...)` — yes, it is.

### Step 3: Verify

- `effect: speed` should work (maps to `minecraft:speed`)
- `effect: minecraft:strength` should work
- `effect: INVALID` should return `null` and the mechanic returns `false`

## Self-Review

- `Registry.POTION_EFFECT_TYPE.get()` is the modern Paper API (post-1.20)
- Old `getByName()` is deprecated and may be removed in future API versions
- Requires lowercase conversion — `effectName.toLowerCase()` + `NamespacedKey.fromString()`
- Backward compatible if existing configs use lowercase effect names; uppercase names need migration
- Update `docs/capabilities.md` if example formats change
