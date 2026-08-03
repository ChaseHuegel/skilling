# ISSUE-188: Rework single-source skills to multi-source sets and apply derived constants (piety source set)

## Context & User Story
- **Goal:** As a skill designer, I want every skill to have a repeatable, multi-source XP set so the 13 single-source skills reach the approved 50 h target with defensible per-action rewards.
- **Agent Role:** You are an expert content/backend engineer executing this task.
- **Inputs:** [REPORT_XP-SOURCE-DESIGN.md](../reports/REPORT_XP-SOURCE-DESIGN.md) §4/§6 (approved), `src/main/resources/skills/*.yml`, `docs/dev/template-skill.yml`.

## Implementation Requirements
- [x] Rework the 13 single-source skills (from REPORT §1) to the §4 expanded source sets (primary + secondary + tertiary), using the tags from §5 where referenced.
- [x] `dual_wield` stays **two-source** (no DualWield dependency, no tertiary per product decision): `entity_damage` `offhand:weapon` + `entity_kill` `offhand:weapon`. It is an accepted outlier — keep per-action rewards sane (current 6/25) and document the residual (~490 h effective, above the 60/action ceiling) in a comment; do not inflate constants.
- [x] `piety` adopts the §6 source set: bury (ISSUE-176, retained), undead kills (`entity_kill` + `state: target_type: #c:undead`), holy-block placement (`block_place` `#c:holy_blocks`), totem (`resurrect`), villager cure (`cure_villager`). Use `state: target_type:` — not `target:` — for the undead source.
- [x] Apply derived reward constants per §3/§4 (`R = 1,666.7 / Σ APM`) to the reworked skills; where a skill is an accepted outlier (alchemy, fishing, armor/tanking, acrobatics), document the residual effective time in a YAML comment instead of silently inflating a single action.
- [x] Add the material tags from §5 to `tags.yml` (`c:holy_blocks`, `c:crops`, `c:foods`, `c:potions`, `c:shields`, `c:heavy_armor` [already present], `c:wooden_products`, `c:shovels`); `c:undead` is an entity tag delivered by ISSUE-186.
- [x] Small supporting engine fix: `ENDER_PEARL` added to `projectileToMaterial` so the acrobatics ender-pearl source (and wizardry's existing one) resolve.
- [x] Deferred to ISSUE-191: applying §4 constants/sources to the already-multi-source skills (archery … wizardry), and the sweep/crit/fish-treasure filters.
- [x] Update `docs/dev/template-skill.yml` only if the schema is unchanged (no new keys — `cure_villager` is a normal trigger key, `target_type` a normal state filter).

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/*.yml`, `src/main/resources/tags.yml`
- **Dependencies:** ISSUE-186 (`target_type` + entity tags) and ISSUE-187 (`cure_villager`) must be merged first — this ticket's piety/undead sources rely on both.
- **Constraints:** Zero hardcoded skills in Java; all content stays in YAML. Bundle skill files are user-customizable, so constants follow the documented back-computation, not the JS.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes (`SkillYamlValidationTest` covers all bundled skills).
- [x] Every `xp_sources` trigger and filter reference resolves against the registries and `tags.yml` (fail-fast load).
- [x] The `cure_villager` and `#c:undead` references load with ISSUE-186/187 merged.

**Note:** The §4 constant pass on the already-multi-source skills (archery … wizardry) is filed as ISSUE-191 per the report's cluster-ticket recommendation; this ticket delivers the single-source rework, piety §6, dual_wield residual, and tag infrastructure.
