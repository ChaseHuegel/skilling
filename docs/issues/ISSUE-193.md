# ISSUE-193: Guard `ThornsDamageMechanic` against synchronous reflect recursion

## Context & User Story
- **Goal:** As a player, I want thorn-reflect abilities to never stack-overflow the server when two players (or player vs. reflected attacker) each have a reflect mechanic.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** Code review of `ThornsDamageMechanic`.

## Implementation Requirements
- [x] Prevent unbounded synchronous recursion: `attacker.damage(damage, player)` inside the victim's `entity_damage_taken` dispatch re-enters the damage pipeline; two reflecting players ping-pong until `StackOverflowError` on the main thread.
- [x] Implement a re-entrancy guard (e.g. a ThreadLocal/static `Set<Entity>` of "currently reflecting" entities that skips a reflect if the attacker is already inside a reflect chain), or defer the reflect off the synchronous path (e.g. schedule on next tick).
- [x] Verify a self-inflicted or reflected chain terminates: a hit → reflect → hit → reflect cycle reflects at most once per entity per incoming hit.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ThornsDamageMechanic.java`
- **Dependencies:** none.
- **Constraints:** Reflect must still fire `EntityDamageByEntityEvent` normally so armor/damage-reduction and other plugins see the reflected damage. The guard state must be cleared in a `finally`/completion path so a later hit reflects again. The mechanic runs on the Bukkit main thread only.
- **Note (resolution):** A static identity-backed `Set<LivingEntity>` tracks entities currently inside a reflect cascade; the attacker is added before `attacker.damage(...)` and removed in a `finally`. A suppressed reflect still returns `true` (activation attempt, per ISSUE-192's contract).

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New unit test simulates a reflect chain (two entities each with a reflect mechanic) and asserts termination without recursion (e.g. via a bounded counter / mocked damage loop), plus that a single reflect still deals damage.
