# ISSUE-199: Aura mechanics must not buff hostile mobs; fix `AllyAuraMechanic` radius-0 edge

## Context & User Story
- **Goal:** As a skill designer, I want `core:field_aura`/`core:aoe_effect` to not apply beneficial effects to hostile mobs by default, and `core:ally_aura` to respect a `radius: 0` config, so auras cannot heal/strengthen monsters or grant a self-buff at radius 0.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] `FieldAuraMechanic` (and `AoeEffectMechanic`) currently apply the effect to every nearby living entity, including hostile mobs; a regen/strength aura heals or empowers monsters. Gate the default to non-hostile targets (allies/neutrals) or make the hostility filter a documented parameter (e.g. `targets: allies|hostiles|all`), with a safe default.
- [ ] `AllyAuraMechanic` applies the effect to the caster even when `radius: 0` (the caster is buffed despite "no allies in range"). Make the self-application respect the configured radius.
- [ ] Keep `CrowdControlMechanic`'s hostile-targeting intent intact (it is an offensive AoE) and document the difference.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/FieldAuraMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AoeEffectMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AllyAuraMechanic.java`
  - `docs/users/capabilities.md`
- **Dependencies:** none.
- **Constraints:** Default behavior change is acceptable (greenfield, no compat obligation per root `AGENTS.md`), but must be documented in Javadoc and `capabilities.md`. Radius cap/clamping already in `AllyAuraMechanic` is reused.

## Verification & Definition of Done
- [ ] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [ ] `./gradlew test` passes.
- [ ] New/updated tests assert hostile mobs are not buffed by default auras and `radius: 0` ally aura does not self-buff.
