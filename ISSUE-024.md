# `ProjectileMechanic` `skilling_damage` metadata — damage parameter is dead code

## Issue

`ProjectileMechanic` stores a `skilling_damage` metadata value on the launched snowball (line 29), and `SkillEventListener.onProjectileHit()` (line 496) reads it to apply damage. However, on modern Paper, snowballs fired by players already trigger `EntityDamageByEntityEvent` with built-in damage. The `onProjectileHit` handler calls `target.damage(damage, shooter)` at `HIGHEST` priority, which may apply duplicate damage or be cancelled by other plugins.

Additionally, the `#minecraft:snowball` projectile item in modern Paper versions deals damage natively, so the custom damage path via metadata may never reach the entity (or applies on top of vanilla damage).

**ISSUES.md reference:** Line 117

## Root Cause

`ProjectileMechanic.execute()` stores damage as snowball metadata, and `onProjectileHit` reads it to call `target.damage()`. In modern Paper, snowballs from `player.launchProjectile(Snowball.class)` already trigger damage events. The `ProjectileHitEvent` handler at HIGHEST priority may run after the damage is already applied, causing double-dipping, or the damage may be cancelled by a protection plugin.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `ProjectileMechanic.java` | `engine/mechanic/impl/ProjectileMechanic.java` | 14, 29-32 (metadata storage) |
| `SkillEventListener.java` | `engine/listener/SkillEventListener.java` | 496-508 (metadata reader) |

## Development Plan

### Step 1: Investigate current behavior

- Launch a snowball via ProjectileMechanic with `damage: 4`
- Check if the snowball deals 4 damage, 0 damage (no effect), or double damage (metadata + native)
- Check if `ProjectileHitEvent` fires before or after `EntityDamageByEntityEvent`

### Step 2: Determine approach based on findings

**Option A** (if metadata path is redundant): Remove metadata storage from `ProjectileMechanic` and the `onProjectileHit` handler entirely. Use `EntityDamageByEntityEvent` with `ProjectileMechanic` hooked via `projectile` trigger type.

**Option B** (if metadata path is the only way): Keep the current approach but add `event.setCancelled(true)` to prevent the snowball from dealing native knockback/damage, ensuring only the custom damage applies.

**Option C**: Use Paper's `EntityDamageByEntityEvent` directly from a `PlayerLaunchProjectileEvent` / projectile hit listener, storing the intended damage in a `Map<UUID, Double>` instead of metadata (thread-safe, avoids PersistentDataContainer overhead).

### Step 3: Clean up dead code paths

- If the metadata-based approach is kept, document why it's necessary
- If removed, delete `onProjectileHit`, the `skilling_damage` metadata string, and the import for `FixedMetadataValue`

## Self-Review

- This issue requires runtime investigation to determine the correct fix — the code paths may work, be redundant, or conflict depending on Paper version
- The `ProjectileHitEvent` handler exists and compiles, so this isn't a build error but a behavioral concern
- A simple test is the best way to determine the correct approach before writing code
