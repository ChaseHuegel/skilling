# ISSUE-302: Engine pieces for a persistent-attribute Survival skill (mechanic, reconcile, cause filter, chunk_load, sleep)

## Context & User Story
- **Goal:** As a skill engine user, I want a persistent, level-scaled attribute modifier mechanic, hardened persistent-mechanic reconciliation, an environmental damage-cause state filter, and exploration/sleep triggers, so a Survival-style skill can grant permanent max-hearts bonuses and respond to fire/drowning/suffocation, new-chunk exploration, and sleeping.
- **Agent Role:** You are an expert Java engineer executing this engine ticket. Content authoring is ISSUE-303.

## Implementation Requirements
- [x] Add `core:persistent_attribute` mechanic implementing `UnlockMechanic`: transient `ADD_NUMBER` attribute modifier under a stable per-ability UUID, replacing any prior modifier with that UUID, idempotent per amount, `amount <= 0` removes the modifier, no removal scheduling, health clamped to the new max after a max-health change.
- [x] Register `core:persistent_attribute` in `Skilling.registerBuiltinMechanics` with load validation: `attribute` required and known, `uuid` required and a valid UUID string, constant `amount` non-negative.
- [x] Harden persistent-mechanic reconciliation in `SkillEventListener.reconcileMilestoneUnlocks`: strip every plugin-owned persistent modifier (marker name) from the player before re-applying active ones, so de-level, reset, and removed/renamed skills recompute from scratch.
- [x] Reconcile persistent unlock mechanics after online `/skills setlevel` (both raise and lower) and `/skills reset` in `SkillsCommand`.
- [x] Add the `cause` state filter (`burn|fire|lava|drowning|suffocation|cactus|starvation`) reading `EntityDamageEvent.getCause()`, failing closed on non-damage events, with load-time value validation in `SkillManager.validateAndWarmState`.
- [x] Add the `chunk_load` trigger (`ChunkLoadEvent`, dispatched only for `isNewChunk()`) registered in `Skilling.registerBuiltinTriggers`, routed through the existing nearby-player dispatch.
- [x] Add the `sleep` trigger (`PlayerDeepSleepEvent`) registered in `Skilling.registerBuiltinTriggers`, routed to the player.
- [x] Update the triggers/state-filter registration used by `TestSkillManager` automatically via `registerBuiltin*`.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/PersistentAttributeMechanic.java` (new)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/ChunkLoadTrigger.java`, `SleepTrigger.java` (new)
- **Dependencies:** `UnlockMechanic` reconciliation path (`SkillEventListener.reconcileMilestoneUnlocks`), `AttributeModifierHelper` conventions, `MechanicParamValidators`.
- **Constraints:** Clean-unplug pillar (transient modifiers only, never persisted in NBT). Pillar V: event-driven, O(1), no per-tick tasks. Fail-fast load validation. Javadoc on all public API elements. No hardcoded skills/levels.

## Verification & Definition of Done
- [x] `PersistentAttributeMechanicTest` covers apply, replace, same-amount no-op, remove on `amount <= 0`, health clamp, null-event (reconcile) path, and missing attribute instance.
- [x] `cause` state filter test covers per-cause matching, fail-closed on non-damage events, and load-time value validation.
- [x] `ChunkLoadTrigger`/`SleepTrigger` registration and dispatch covered (trigger-index + listener tests).
- [x] Reconcile test covers level-scaled numeric params at current level and the stale-strip pass removing an orphaned modifier.
- [x] Online `setlevel` raise/lower and `reset` trigger reconciliation; offline path caught up on join.
- [x] `./gradlew build` and `./gradlew test` pass.