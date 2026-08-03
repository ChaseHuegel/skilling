# ISSUE-195: Add a damager-role guard to `ModifyDamageMechanic`

## Context & User Story
- **Goal:** As a skill designer, I want `core:modify_damage` to only ever scale the activating player's outgoing damage, so binding it to the wrong trigger cannot silently multiply incoming damage on the player.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** Code review of `ModifyDamageMechanic`.

## Implementation Requirements
- [x] Before scaling `damageEvent.getDamage()`, verify the activating player is the attacker, mirroring the other damage mechanics via `EntityDamageResolver.resolveDamagerPlayer(event)`; return `false` (no-op) otherwise.
- [x] This makes the mechanic's behavior independent of the trigger it is bound to (`entity_damage` vs. `entity_damage_taken`).

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyDamageMechanic.java`
- **Dependencies:** none.
- **Constraints:** Existing `entity_damage`-bound configs behave identically (the dispatcher already resolves the player damager). Only the misbound case changes (from "scales wrong damage" to "no-op").

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New unit test: `ModifyDamageMechanic` scales damage when the player is the damager and returns `false`/does not modify when the player is the damaged entity.
