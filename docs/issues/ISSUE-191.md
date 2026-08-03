# ISSUE-191: Apply §4 constants and source set to the remaining multi-source skills

## Context & User Story
- **Goal:** As a skill designer, I want every already-multi-source skill to reach the approved 50 h target with the §4 source sets and derived constants, so the balance pass is complete for all 32 skills.
- **Agent Role:** You are an expert content engineer executing this task.
- **Inputs:** [REPORT_XP-SOURCE-DESIGN.md](../reports/REPORT_XP-SOURCE-DESIGN.md) §4 (approved). ISSUE-188 delivered the single-source rework (12 skills) + piety §6 + tags; this ticket covers the remaining skills.

## Implementation Requirements
- [ ] For the skills NOT reworked by ISSUE-188 (archery, bard, building, carpentry, cooking, dual_wield [2-source residual kept], enchanting, heavy_weapons, husbandry, light_weapons, masonry, mining, one_handed, riding, smithing, tailoring, throwing, unarmed, wizardry), apply the §4 source sets and the uniform derived constant `R = 1,666.7 / Σ APM` per skill.
- [ ] Where a §4 source needs an engine filter that does not yet exist, substitute a functional source of the same APM class (keeping R unchanged) and document, or file an engine ticket for the filter:
  - `heavy_weapons` sweep hit (`entity_damage` sweep) — no sweep state filter.
  - `light_weapons` crit hit — no crit state filter.
  - `fishing` treasure catch — no fish-result state filter.
  - `dual_wield` stays 2-source per the product decision (ISSUE-188); do not add a tertiary.
- [ ] Reconcile `cooking`/`masonry`/`smithing` furnace sources with the per-item scalar (REPORT §3): `furnace_extract` pays per extracted item.
- [ ] Respect the §2 outlier ceiling: for outlier skills add high-frequency sources first, then document the residual; do not inflate a single action absurdly.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/{archery,bard,building,carpentry,cooking,enchanting,heavy_weapons,husbandry,light_weapons,masonry,mining,one_handed,riding,smithing,tailoring,throwing,unarmed,wizardry}.yml`
- **Dependencies:** ISSUE-186 (entity tags) and ISSUE-187 (`cure_villager`) are merged. The engine filters for sweep/crit/treasure may require new state filters (engine ticket).
- **Constraints:** Zero hardcoded skills in Java; all content stays in YAML. Bundled skill files remain user-customizable.

## Verification & Definition of Done
- [ ] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [ ] `./gradlew test` passes (`SkillYamlValidationTest` sweeps all bundled skills).
- [ ] Every §4 source set for the listed skills is present with the derived constant; exceptions documented.
