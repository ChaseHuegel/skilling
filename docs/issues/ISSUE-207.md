# ISSUE-207: Consolidate the three identical damage-cancel mechanics

## Context & User Story
- **Goal:** As a maintainer, I want a single chance-based damage-cancel implementation so `core:dodge`, `core:block_damage`, and `core:cancel_damage` are documented aliases or distinct behaviors instead of three byte-identical classes.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `BlockDamageMechanic`, `CancelDamageMechanic`, and `DodgeMechanic` all extend `BaseDamageCancelMechanic` with the identical `getChance` (percentage roll to cancel damage). Decide whether the three keys should stay (e.g. as aliases/config distinctions) or collapse to one implementation with aliased registration.
- [x] If kept as separate keys, give each a distinct, documented semantic (or note them as aliases in `capabilities.md`); if collapsed, register the other keys as aliases pointing at the single class and update all skill YAML that references them.
- [x] Update `docs/users/capabilities.md` and any bundled skill YAML referencing the affected keys.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{BlockDamage,CancelDamage,Dodge}Mechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (registration)
  - `src/main/resources/skills/**` (if keys change)
  - `docs/users/capabilities.md`
- **Dependencies:** none.
- **Constraints:** Greenfield — removing/aliasing keys is acceptable; keep the registered keys from `Skilling.java` (lines 284, 294, 301) consistent with the chosen design. No behavior change to the roll itself.
- **Note (resolution):** Collapsed to a single concrete `DamageCancelMechanic` (merging the base roll + `chance` param); deleted `BaseDamageCancelMechanic`, `DodgeMechanic`, `BlockDamageMechanic`, `CancelDamageMechanic`. All three keys — `core:dodge`, `core:block_damage`, `core:cancel_damage` — are registered as aliases pointing at it, so no bundled skill YAML changes were needed. The ISSUE-192 chance tests consolidated into `DamageCancelMechanicTest`; `capabilities.md` documents the three keys as flavor aliases.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] All keys referenced by bundled skill YAML resolve after the change (`SkillYamlValidationTest`).
