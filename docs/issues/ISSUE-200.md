# ISSUE-200: Make `ShieldDisableMechanic` target semantics independent of trigger binding

## Context & User Story
- **Goal:** As a skill designer, I want `core:shield_disable` to disable a clearly-defined target's shield regardless of which trigger it is bound to, so binding it to `entity_damage` vs. `entity_damage_taken` does not silently flip between "disable the enemy's shield" and "disable your own shield".
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Define and implement an explicit target rule. Current behavior: on `EntityDamageByEntityEvent` the cooldown is applied to `de.getEntity()` (the damaged party); on `PlayerInteractEvent` to the activating player. Because the mechanic fires for whichever trigger the ability binds, the *same* mechanic disables the enemy (bound to `entity_damage`) or the player's own shield (bound to `entity_damage_taken`).
- [x] Make the target explicit via a documented parameter (e.g. `target: attacker|victim|self`) with a safe default, or always target a single role and document it.
- [x] Add the missing `instanceof Player` guard / null checks for the damaged entity.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ShieldDisableMechanic.java`, `docs/users/capabilities.md`
- **Dependencies:** none.
- **Constraints:** Greenfield — behavior change is fine; document it. Cooldown applies via `Player#setCooldown(Material.SHIELD, ticks)` as today.
- **Note (resolution):** Added a `target` param (`victim` default | `attacker` | `self`). On `EntityDamageByEntityEvent`, `victim` targets the damaged player, `attacker` targets the player attacker via `EntityDamageResolver.resolveDamagerPlayer` (projectile shooters count), and `self` targets the activating player; off the damage path everything falls back to the activating player. A non-player victim/attacker resolves to a safe no-op.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New/updated tests assert the shield cooldown is applied to the documented target for both `entity_damage` and `entity_damage_taken` bindings, and that a non-player damaged entity is a safe no-op.
