# ISSUE-184: Add a `projectile_hit` trigger and rebind the throwing skill's `return_chance` to it

## Context & User Story
- **Goal:** As a player with the Throwing skill, I want the Return Chance ability to actually recover thrown tridents, so the ability isn't a dead no-op.
- **Agent Role:** You are an expert backend (Paper) engineer executing this task.

The Throwing skill's `return_chance` ability declares `trigger: "launch_projectile"`, but `core:projectile_return` only acts on `ProjectileHitEvent` (it returns `false` for any other event). Because the ability fires on *launch*, the mechanic never runs and the ability does nothing. Introduce a `projectile_hit` trigger and rebind the ability to it.

## Implementation Requirements
- [ ] Add a `ProjectileHitTrigger` (`getKey()` → `projectile_hit`, event class `ProjectileHitEvent`) and register it in `TriggerRegistry` via `Skilling.registerBuiltinTriggers()`.
- [ ] Add a dispatch handler in `SkillEventListener` for `ProjectileHitEvent` that routes the `projectile_hit` trigger when the projectile's shooter is a player. Avoid conflicts with the existing HIGHEST `onProjectileHit` handler used for `ProjectileMechanic` damage.
- [ ] Update `throwing.yml` `return_chance` to `trigger: "projectile_hit"` and remove the stale NOTE comment.
- [ ] Extend `SkillEventListener.resolveEventMaterial()` so `ProjectileHitEvent` resolves the projectile material (e.g., trident → `minecraft:trident`), enabling `target` filters on hit-based XP sources/abilities.
- [ ] Add unit test coverage: trigger registration, dispatch of `projectile_hit`, and that `return_chance`'s mechanic is reachable through the new trigger.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/ProjectileHitTrigger.java` (new; mirror `LaunchProjectileTrigger.java`)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` — `registerBuiltinTriggers()` (~line 319)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` — new handler + `resolveEventMaterial()` (~line 733)
  - `src/main/resources/skills/throwing.yml` — `return_chance` trigger + NOTE comment (line 29)
  - `src/test/**` — mirror existing trigger/mechanic tests
- **Dependencies:** `ProjectileReturnMechanic` (already implements hit-time behavior), `SkillTrigger` interface.
- **Constraints:**
  - Do not regress the `ProjectileMechanic` damage path (HIGHEST handler at `SkillEventListener.java:658`).
  - A hit with no entity or a null hit target may still be a valid trigger firing (projectile landed on a block); decide and document whether those dispatch.
  - Keep the trigger key consistent across YAML, registry, and web registries endpoint.

## Verification & Definition of Done
- [ ] `./gradlew build` and `./gradlew test` pass
- [ ] In-game: with Return Chance unlocked, thrown tridents return to the player with the configured chance; the ability fires on hit, not on launch
- [ ] No regression to Skilling-launched projectile damage
- [ ] `resolveEventMaterial` returns the trident material for `ProjectileHitEvent` so target filters work
