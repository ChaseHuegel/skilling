# ISSUE-250: core:unlock_recipe Milestone Unlock Mechanic

## Context & User Story
- **Goal:** As a skill author, I want a `core:unlock_recipe` mechanic that permanently unlocks any plugin, datapack, or vanilla recipe by its namespaced key. I want to bind it to a skill milestone so it fires once as a passive one-time unlock as part of skill progression, without it fitting the semantics of a costed, cooldown-gated active ability.
- **Agent Role:** You are an expert backend engineer executing this task. The mechanic unlocks recipes through the player's persistent recipe book (`Player#discoverRecipe`). One-time semantics come from the mechanic's idempotency (`Player#hasDiscoveredRecipe`) plus a join/reload reconciliation pass that makes unlocks retroactive for players already past the milestone.

## Implementation Requirements
- [x] Add an `UnlockMechanic` marker interface (skilling-api) that extends `SkillMechanic` and marks persistent, one-time unlock mechanics. Document that the `execute` event may be `null` during join/reload reconciliation.
- [x] Add `MechanicRegistry.isUnlock(String key)` backed by the registered mechanic class, so callers can identify unlock mechanics without instantiating them.
- [x] Add a `core:unlock_recipe` mechanic (src) with a `recipe` namespaced-key parameter. It must no-op (return `false`) when the recipe is unregistered or already discovered, and `discoverRecipe` + return `true` only on a real unlock.
- [x] Load-time validation via a new `MechanicParamValidators.recipe(...)`: require a constant present string, reject malformed namespaced keys (fail-fast), and soft-warn (do not throw) when the recipe is not currently registered, since third-party plugins may register recipes after Skilling loads.
- [x] Reconcile unlocks on `PlayerJoinEvent` and after `/skills reload` for online players: for each skill, for each `trigger: level_up` ability whose owning-skill level meets `unlock_level`, execute its `isUnlock` mechanics (bypassing requirements/cooldown/feedback). Silent by design.
- [x] Register the mechanic in `Skilling.registerBuiltinMechanics` and wire the recipe-existence lookup predicate into `MechanicParamValidators.configureLookups`.

## Technical Specifications & Context
- **Target Files:**
  - New: `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/mechanic/UnlockMechanic.java`, `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/UnlockRecipeMechanic.java`, `src/test/.../engine/mechanic/impl/UnlockRecipeMechanicTest.java`, `src/test/.../engine/listener/SkillEventListenerReconcileUnlocksTest.java`
  - Modified: `skilling-api/.../engine/registry/MechanicRegistry.java`, `src/.../engine/mechanic/impl/MechanicParamValidators.java`, `src/.../Skilling.java`, `src/.../engine/listener/SkillEventListener.java`, `src/.../engine/listener/PlayerListener.java`, `src/.../engine/lockdown/LockdownManager.java`, `docs/users/capabilities.md`, `docs/users/creating-skills.md`, `docs/users/api-integration.md`, `docs/dev/template-skill.yml`, `docs/dev/DESIGN.md`, `docs/dev/SKILL-DESIGN-FRAMEWORK.md`
- **Dependencies:** existing `level_up` trigger (`LevelUpTrigger`/`SkillingLevelUpEvent`), `SkillEventListener.evaluateParams`, `MechanicParamValidators.configureLookups` predicate injection pattern.
- **Constraints:** Per `src/AGENTS.md`: fail-fast parsing; mechanics are prototype-scoped records; return `true` only on an activation attempt. Per `skilling-api/AGENTS.md`: Javadoc required on all public API elements; additive API change (no breaking marker). Reconciliation must run on the main thread (join/reload hooks are already main-thread).

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including new tests: `UnlockRecipeMechanicTest` (unlock/no-op on already-discovered/missing-recipe), `MechanicParamValidatorsTest` additions (missing/malformed/soft-warn), `MechanicRegistry.isUnlock`, and `SkillEventListenerReconcileUnlocksTest` (past-milestone unlocks, below-milestone does not).
- [x] A skill YAML with `trigger: level_up` + `unlock_level` + `core:unlock_recipe` parses; a malformed `recipe` key or unknown param fails the reload.
- [x] DOX pass complete (docs/users + docs/dev updated; capabilities catalog entry added).
