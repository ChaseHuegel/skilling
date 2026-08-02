# ISSUE-179: Skill-level lore lines are dropped when saving from the web skill editor

## Context & User Story
- **Goal:** As an admin, I want to edit a skill's display lore lines in the skill editor's Display section and have them written to the skill file when I save and reload, just like ability lore works.
- **Agent Role:** You are an expert frontend (Vue 3) engineer executing this task.

Lore lines added in the Display section are not written to the file after save + reload. Ability lore persists fine; only the skill-level `display.lore` is affected.

## Implementation Requirements
- [x] Include the skill-level `lore` in the PUT/POST payload from both save paths in `SkillEditorPage.vue` (`save()` and `leaveSave()`).
- [x] Convert the editor's `_key`-enriched lore rows back to plain strings before sending — never send the client-only `_key` field to the backend.
- [x] Ensure loading an existing skill with lore and saving without touching lore preserves it (no data loss on round-trip).
- [x] Add E2E coverage: add lore lines in the Display section, apply & reload, verify the lore is present in the YAML file and in the editor after reload.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/views/SkillEditorPage.vue` — `save()` (~line 402) and `leaveSave()` (~line 452) build `payload` without a `lore` key; compare with `formAbilityToApi()` which does emit `display.lore`.
  - `web/frontend/src/components/skills/DisplaySection.vue` — lore rows become `{ _key, text }` objects (`enrichFormKeys` in `SkillEditorPage.vue:290-292` converts the string array).
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java` — `toMap()` writes `display.lore` only when non-empty (line 111-113); the missing payload field therefore produces a file with no skill lore.
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillDetailDTO.java`
- **Dependencies:** `stripRowKeys()` helper, staging workflow (`StagingManager`), existing ability-lore path as the working reference.
- **Constraints:**
  - The backend rejects unknown JSON properties, so `_key` must be stripped before the payload is sent.
  - Keep `isDirty` semantics unchanged (do not let key enrichment flip the dirty flag).

## Verification & Definition of Done
- [x] `cd web/frontend && npm run build` passes
- [x] E2E: add a lore line → save → apply & reload → lore present in the YAML and in the editor after reload
- [x] Round-trip: save an existing skill with lore without editing it — lore is preserved
- [x] Ability lore still works (no regression)
