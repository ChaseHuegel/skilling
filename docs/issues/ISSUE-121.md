# ISSUE-121: Run damage-cancelling mechanics at an early priority instead of MONITOR

**Status:** Open
**Type:** Bug
**Severity:** High (dodge/block/cancel damage easily defeated and processed too late)

---

## Context & User Story

- **Goal:** As a player, I want dodge and damage-cancel abilities to reliably negate the damage they are configured to prevent, regardless of other plugins.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Register the `entity_damage_taken` dispatch handlers at an early priority (LOWEST/HIGHEST) **without** `ignoreCancelled=true`
- [ ] Verify `DodgeMechanic`, `BlockDamageMechanic`, and `CancelDamageMechanic` still cancel the `EntityDamageEvent` and that cancellation is respected by the new handler ordering
- [ ] Fix the class Javadoc (`BaseDamageCancelMechanic.java:54-58`) which incorrectly claims handlers run at HIGHEST
- [ ] Ensure the firework/projectile handlers that legitimately run at HIGHEST are unaffected
- [ ] Add tests verifying a dodge roll at LOWEST cancels before other plugins observe the damage

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:146-151` (handler registration)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/BaseDamageCancelMechanic.java:20-31`
- **Dependencies:** `DodgeMechanic`, `BlockDamageMechanic`, `CancelDamageMechanic` subclass `BaseDamageCancelMechanic`.
- **Constraints:** Do not break the event-cancellation semantics or allow post-cancellation dispatch.

### Root Cause

All `entity_damage_taken` dispatch handlers run at `MONITOR` with `ignoreCancelled=true`. Damage-cancelling mechanics then call `setCancelled(true)` — but at MONITOR the event has already been processed by every other plugin, and any earlier plugin cancellation suppresses the dodge roll entirely. The Javadoc's claimed HIGHEST priority is false.

### Proposed Fix

Move the damage-related dispatch handlers to an early priority (LOWEST) without `ignoreCancelled` so the dodge/block/cancel decision happens before other plugins act, and cancellation propagates normally. Update the Javadoc to match reality.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass
- [ ] Unit test: a dodge that cancels the event does so before lower-priority handlers run
- [ ] Unit test: a previously-cancelled damage event does not dispatch dodge abilities
- [ ] Class Javadoc matches the actual handler priority
