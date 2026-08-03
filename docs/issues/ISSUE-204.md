# ISSUE-204: Reduce per-event overhead on the ability/XP hot path

## Context & User Story
- **Goal:** As a server owner, I want the engine to keep 20 TPS under heavy load, with no per-event reflection instantiation and no full registry scan per dispatch.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Cache mechanic constructors instead of reflection per call: `MechanicRegistry.create` invokes `clazz.getDeclaredConstructor().newInstance()` for every mechanic execution per event (`MechanicRegistry.java`). Resolve the constructor once at registration (fail-fast already guarantees a public no-arg constructor) and instantiate via the cached constructor, or pre-build a supplier per key.
- [ ] Add a trigger-key index so `SkillEventListener#grantXp` and `#fireAbilities` do not scan every skill × every source/ability per event filtering by trigger. Build a `triggerKey → [skills/sources/abilities]` index once per skill load (rebuilt on `/skills reload`) so a dispatch only visits the entries bound to that trigger.
- [ ] Keep `evaluateParams`/param evaluation allocation reasonable (reuse or lazily build where safe).

## Technical Specifications & Context
- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/MechanicRegistry.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java` (index build)
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java` (index rebuild on reload)
- **Dependencies:** none.
- **Constraints:** Thread-safe publication of the index (reuse the existing immutable-snapshot swap pattern in `SkillManager`). No behavior change — same skills, triggers, and rewards fire in the same order.

## Verification & Definition of Done
- [ ] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [ ] `./gradlew test` passes (existing `SkillEventListener*` and registry tests cover dispatch behavior).
- [ ] New test asserts registry `create` no longer reflects per call (or that the index yields the same dispatch results as the scan).
- [ ] Reload rebuilds the trigger index without stale entries (`LockdownManagerReloadTest` pattern).
