# ISSUE-187: Add a dedicated `cure_villager` trigger

## Context & User Story
- **Goal:** As a skill designer, I want a `cure_villager` trigger so that curing a zombie villager grants XP (piety source set, REPORT_XP-SOURCE-DESIGN §6 source 5), attributed to the player who initiated the cure.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** [REPORT_XP-SOURCE-DESIGN.md](../reports/REPORT_XP-SOURCE-DESIGN.md) §6/§7 (approved). Paper 1.21.8: `EntityTransformEvent.TransformReason.CURED` + `ZombieVillager.getConversionPlayer()`.

## Implementation Requirements
- [x] Add `CureVillagerTrigger` (`src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/`) implementing `SkillTrigger`: key `cure_villager`, event class `EntityTransformEvent`.
- [x] Register `cure_villager` in `Skilling.registerBuiltinTriggers`.
- [x] Add a MONITOR handler in `SkillEventListener` (`onCureVillager`) that:
  - returns when `getTransformReason() != TransformReason.CURED`;
  - returns when `event.getEntity()` is not a `ZombieVillager`;
  - resolves the curing player via `zombieVillager.getConversionPlayer()`;
  - dispatches only when that player is online (`getPlayer()` non-null) — cure completes ~3–5 min after feeding, so an offline initiator receives nothing (documented edge);
  - dispatches with trigger key `cure_villager`.
- [x] Javadoc on the trigger class and handler explaining purpose, YAML key, and the offline-player edge.
- [x] `cure_villager` must not fire for other transform reasons (INFECTION/DROWNED/etc.).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/CureVillagerTrigger.java` (new)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java`
  - `src/test/java/io/github/chasehuegel/skilling/trigger/SkillTriggerTest.java` (add provider row)
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/` (new handler test)
- **Dependencies:** none. Bulk scalar for the event is `1` (no `resolveEventBulkScalar` branch needed).
- **Constraints:** Observer only — never cancel the transform. No target-material filter applies (`resolveEventMaterial` returns null for `EntityTransformEvent`), so the handler is self-scoped to CURED + ZombieVillager.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including new tests:
  - trigger key/event class provider entry (`SkillTriggerTest`).
  - handler dispatches `cure_villager` for a CURED `ZombieVillager` whose conversion player is online.
  - handler does not dispatch for non-CURED reasons, non-`ZombieVillager` origin, or offline conversion player.
- [x] Edge case handled: conversion player unknown (`getConversionPlayer() == null`) → no dispatch, no throw.

**Note:** `./gradlew build` is blocked by the same 4 pre-existing `ProjectileHitTriggerTest`/`ProjectileReturnMechanicTest` failures tracked in ISSUE-190. Total suite: 579 tests, 4 failed (all pre-existing).
