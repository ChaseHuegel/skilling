# ISSUE-198: Complete mechanic Javadoc and fix registry-key documentation drift

## Context & User Story
- **Goal:** As an addon or config author, I want every mechanic's class Javadoc to state its exact registered YAML key, required/optional params, and purpose, so `docs` and code agree and `src/AGENTS.md` §7 is satisfied.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Add class-level Javadoc (purpose, YAML key, required/optional parameters) to mechanics currently missing it: `FieldAuraMechanic`, `ModifyDamageMechanic`, `ModifyEnchantCostMechanic`, `RepairDiscountMechanic`.
- [x] Fix YAML keys in Javadoc that omit the `core:` prefix they are actually registered under in `Skilling.java` (e.g. `aoe_effect` → `core:aoe_effect`, and the same for `ProjectileMechanic` (`projectile`), `ModifyAttributeMechanic`, `ModifyBrewTimeMechanic`, `ModifyPotionDurationMechanic`, `SaturationInjectMechanic`, `ModifyCraftOutputMechanic`, `YieldMultiplierMechanic`, `ModifyFurnaceOutputMechanic`).
- [x] Cross-check the documented YAML keys in every mechanic against the registrations in `Skilling.java` (mechanic keys + parameter names) and reconcile all mismatches.
- [x] If any drift affects user docs, update `docs/users/capabilities.md` in the same change.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/*.java`, `src/main/java/io/github/chasehuegel/skilling/Skilling.java`, `docs/users/capabilities.md`
- **Dependencies:** none.
- **Constraints:** No behavior change. Doc-only edit; keys referenced by bundled skill YAML must remain valid (only the `core:` prefix in comments changes).
- **Note (resolution):** Added class Javadoc to `ModifyEnchantCostMechanic` and `RepairDiscountMechanic` (`FieldAuraMechanic`/`ModifyDamageMechanic` gained theirs in ISSUE-199/195). Fixed the `core:` prefix on 11 mechanics (apply_status, cancel_damage, modify_attribute, modify_brew_time, modify_craft_output, modify_furnace_output, modify_potion_duration, projectile, saturation_inject, teleport, yield_multiplier). A script confirmed all 45 registered keys are documented exactly; `capabilities.md` already uses `core:` on all 45 entries.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures; `skilling-api` javadoc jar builds).
- [x] Every `SkillMechanic` implementation has a class-level Javadoc with its registered key and parameters.
- [x] Every documented key matches `Skilling.java` registration exactly.
