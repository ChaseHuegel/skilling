# ISSUE-129: Resolve projectile damagers in damage mechanics

**Status:** Resolved
**Type:** Bug
**Severity:** Medium (damage abilities silently no-op for projectile attacks)

---

## Context & User Story

- **Goal:** As a player, I want lifesteal, execute, thorns, crowd-control, and status abilities to apply when I deal damage with a bow or snowball, not only with direct melee hits.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] In the mechanics that check `de.getDamager().equals(player)` (`ApplyStatusMechanic`, `CrowdControlMechanic`, `ExecuteMechanic`, `LifestealMechanic`, `ThornsDamageMechanic`), resolve the effective player from a projectile's shooter (mirroring `SkillEventListener.resolvePlayerDamager`)
- [x] Ensure `entity_damage` abilities fire consistently with how the dispatch resolves the player
- [x] Add unit tests covering: arrow/snowball damage resolves the shooter as the player; melee damage unchanged

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:134-138` (`resolvePlayerDamager`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{ApplyStatusMechanic,CrowdControlMechanic,ExecuteMechanic,LifestealMechanic,ThornsDamageMechanic}.java`
- **Dependencies:** `EntityDamageByEntityEvent`.
- **Constraints:** Consider a shared helper so the five mechanics do not each duplicate shooter-resolution logic.

### Root Cause

The dispatch resolves the player from a projectile's shooter, so `entity_damage` abilities fire for arrows/snowballs — but the mechanics then compare `de.getDamager().equals(player)`, which is false when the damager is the projectile. The ability silently no-ops (`execute` returns false, no consume).

### Proposed Fix

Add a shared resolution (e.g. static helper returning the owning player from the damager or projectile shooter) and use it in each mechanic's damager check.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: arrow hit resolves shooter as the player for lifesteal/execute
- [x] Unit test: melee hit behavior is unchanged
- [x] Unit test: non-player damager (mob projectile) returns false
