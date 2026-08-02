# ISSUE-170: Fix cooldown display on ability cards (row shows on all abilities and renders raw evaluator JSON)

**Status:** Closed
**Type:** Bug
**Severity:** Medium (UI display bug on the Abilities page — no data loss or exploit)

---

## Context & User Story

- **Goal:** As an admin, I want the Abilities page cards to show a `Cooldown` row only for abilities that actually have a cooldown, formatted as `Cooldown: {n}s` (e.g. `Cooldown: 5s`), never as raw JSON like `Cooldown: { "type": "constant", "params": { "value": 0 } }s`.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [x] Render the `Cooldown` row only when the ability's effective cooldown is greater than 0 (currently the row renders for every ability, including zero-cooldown passives)
- [x] Display the formatted numeric value (`{n}s`), not the raw evaluator object (currently `{{ ability.requirements.cooldown }}` JSON-stringifies the evaluator)
- [x] Handle non-constant evaluator cooldowns (e.g. milestone/linear) without rendering raw JSON — either evaluate a representative value or show a label such as the evaluator type
- [x] Reuse/share the cooldown-to-number logic that already exists in `SkillEditorPage.cooldownToNumber` instead of duplicating it
- [x] Add a test (unit or E2E) covering: zero/absent cooldown shows no row; a constant cooldown of 5 renders `Cooldown: 5s`

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/components/skills/AbilityCard.vue:22-24` (the `v-if` and the raw interpolation)
  - `web/frontend/src/views/AbilitiesPage.vue:75-79` (the `cooldown?: number` type annotation is wrong — the value is an evaluator object)
  - `web/frontend/src/views/SkillEditorPage.vue:289-296` (existing `cooldownToNumber` helper to share)
  - Backend root cause for reference: `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java:380-388` — `parseCooldown` returns `EvaluatorDTO("constant", {value: 0.0})` for a missing `cooldown` key, so the API always returns a truthy evaluator object
- **Dependencies:** Related to ISSUE-104 (web API accepts scalar cooldown values); the API contract intentionally returns an `EvaluatorDTO` for `requirements.cooldown` (`SkillDetailDTO.java:76,81-83`), so the fix belongs in the frontend display layer.
- **Constraints:** Do not change the API DTO shape (the editor depends on the evaluator form for editing). The card is read-only.

### Root Cause

Two compounding defects:

1. `SkillSerializer.parseCooldown` returns `new EvaluatorDTO("constant", Map.of("value", 0.0))` when a skill's YAML has no `cooldown` key (`SkillSerializer.java:387`). Because the DTO always carries an evaluator object, `GET /api/skills/{id}` returns `requirements.cooldown` as the object `{ "type": "constant", "params": { "value": 0 } }` for **every** ability.
2. `AbilityCard.vue:22` guards with `v-if="ability.requirements?.cooldown"`, which is always truthy for an object — so the row renders for all abilities, even passives with no cooldown. Line 24 then interpolates the object directly: `{{ ability.requirements.cooldown }}s`. Vue's `toDisplayString` JSON-stringifies objects, producing `{ "type": "constant", "params": { "value": 0 } }s` instead of a formatted number.

### Proposed Fix

Add a small display helper (extracted from/next to `SkillEditorPage.cooldownToNumber`) that turns a cooldown evaluator into a displayable number or `null`:
- constant evaluator → `params.value` (only show the row when `> 0`)
- number → itself
- milestone/linear or unknown → show a stable label (e.g. the evaluator `type`) or omit, but never raw JSON

Bind the card's `v-if` to the helper's non-zero result and render `{{ formatted }}s` (or `${formatted}s`). Correct the `cooldown?: number` annotation in `AbilitiesPage.vue` to reflect the evaluator object shape.

### Resolution

New shared `web/frontend/src/utils/cooldown.ts` provides `cooldownToNumber` (moved out of `SkillEditorPage`, which now imports it) and `cooldownLabel`, which returns `"5s"` for positive constants, `null` for zero/absent cooldowns, and a stable `"<type> (dynamic)"` label for non-constant evaluators. `AbilityCard` binds the Cooldown row's `v-if` to the label result and renders it directly, and its `isActive` computation now uses `cooldownToNumber` (the old `(cooldown ?? 0) > 0` compared an object, always false — a latent Active/Passive badge bug). The `cooldown?: number` annotations in `AbilityCard` and `AbilitiesPage` were corrected to the evaluator shape.

## Verification & Definition of Done

- [x] `cd web/frontend && npm run build` passes
- [x] Test: an ability with no cooldown (constant value 0 or absent) shows no `Cooldown` row (E2E: Geologist has no row)
- [x] Test: an ability with a constant cooldown of 5 renders `Cooldown: 5s` (E2E: Vein Miner shows `5s`)
- [x] Test: a non-constant cooldown evaluator renders no raw JSON (E2E: no card contains `"type"`/`"params"` text)
- [x] Manual smoke: open the Abilities page — only true-cooldown abilities show the row, formatted correctly (covered by the E2E test; full suite 79/79 pass)
