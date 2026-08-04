# ISSUE-222: Web editor destroys non-scalar cooldown requirements

## Context & User Story
- **Goal:** As a server admin, I want the ability editor to preserve (and ideally edit) evaluator-based cooldown requirements instead of collapsing them to `0` on load and dropping them on save.
- **Agent Role:** You are an expert full-stack engineer executing this task.

## Implementation Requirements
- [ ] Stop collapsing cooldown evaluators to a bare number. Today `cooldownToNumber` (`web/frontend/src/utils/cooldown.ts:18-25`) returns `0` for any evaluator without a numeric `params.value` (e.g. `{type: "linear", params: {base, step}}`), and `apiAbilityToForm` (`SkillEditorPage.vue:325`) applies it on load — so a linear/milestone cooldown becomes `0`, `isAbilityActive` (`AbilityCard.vue:135-138`) labels the ability "Passive", and the save payload writes `cooldown: 0` (the engine drops it, `SkillSerializer.abilityToMap:281-287`).
- [ ] Preserve the raw cooldown evaluator through the load/save round trip when it is not a plain constant.
- [ ] Add an evaluator editor for the cooldown requirement (reuse the `EvaluatorParameter`-style type/params editing, or a purpose-built constant/linear/milestones picker) so admins can set and change evaluator cooldowns. Plain constant cooldowns should keep the current scalar input.
- [ ] Keep `cooldownLabel` behavior (display only) consistent with the new editor.
- [ ] Add an E2E or component-level regression test: an ability with `requirements.cooldown: { linear: { base: 5, step: -0.02, max: 1 } }` loads showing a cooldown, saves, and the staged YAML still contains the linear cooldown block.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/utils/cooldown.ts`
  - `web/frontend/src/views/SkillEditorPage.vue` (`apiAbilityToForm`)
  - `web/frontend/src/components/skills/AbilityCard.vue` (cooldown field + active detection)
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java` (`abilityToMap` / `parseRequirements` already support evaluator cooldowns)
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:370-379` (engine already parses evaluator cooldowns)
- **Dependencies:** none.
- **Constraints:** The engine and web DTO already support evaluator cooldowns end-to-end; only the frontend collapses them. `docs/dev/template-skill.yml:107-109` documents the linear cooldown form — keep that supported.

## Verification & Definition of Done
- [ ] A skill with a linear/milestone cooldown loads, displays, saves, and reloads with the evaluator intact.
- [ ] Plain constant cooldowns still work as a scalar number.
- [ ] `cd web/frontend && npm run build` passes.
- [ ] `./gradlew build` and `./gradlew test` pass.
