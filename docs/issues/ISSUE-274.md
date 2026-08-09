# ISSUE-274: Web — Skill Editor Support for Referenced Base/Shared Abilities

## Context & User Story
- **Goal:** As a server admin, I want to open a skill in the web editor that references a reusable base ability (a bare `- id: "vein_miner"` entry pointing at `abilities/vein_miner.yml`), edit the skill, and save it, without the editor forcing me to inline the ability or detaching the skill from the shared base. Edits to the skill must only ever apply to that skill's own yml, never to the base ability file.
- **Agent Role:** You are an expert frontend engineer executing this task. The backend already isolates edits to the skill's own yml (the web layer has no code path that writes to `abilities/`), and the serializer already round-trips reference-shaped entries. The work is frontend-only: teach the editor to recognize, display, validate, and re-serialize referenced abilities correctly.

## Implementation Requirements
- [x] **Detect references:** in `apiAbilityToForm` / `enrichFormKeys` (`SkillEditorPage.vue`), identify a reference-shaped ability (has `id`, `trigger` is blank/null) and mark it explicitly in the editor model (e.g. a client-only `isReference` / `baseAbilityId` flag) so the UI and save path can branch on it. Currently a reference loads as a bare card with an empty trigger field and no indication it is a base/shared ability.
- [x] **Relax validation:** `validate()` (`SkillEditorPage.vue` lines 149-163) requires a non-blank `trigger` on every ability and currently blocks saving any skill that references a base ability. A referenced ability must be valid without a trigger. Keep the id-required, duplicate-id, and skill-id checks.
- [x] **Serialize references correctly:** `formAbilityToApi` (lines 386-406) currently expands an ability into a full object. For a referenced ability, emit only `id` plus any fields the user explicitly changed (matching `SkillSerializer.abilityToMap`'s omission rules: no `trigger`, no empty `display`/`requirements`/`mechanics`/`feedback`), so the skill yml keeps a bare reference with real overrides only. A no-op open/save round-trip must not add spurious overrides.
- [x] **Mark overrides explicitly:** when a user edits a field of a referenced ability (e.g. changes `unlock_level` or adds a lore line), that field is an intentional per-skill override and must be written to the skill yml; unchanged inherited fields must be omitted. Make the override semantics visible in the UI (e.g. a "Base/Shared" badge plus clear behavior on edited fields).
- [x] **Display:** in `AbilitiesSection.vue`, render a referenced ability distinctly (badge/label such as "Base/Shared"), show that inherited fields come from the shared file, and prevent or clearly signal the difference between inheriting a field and overriding it. Do not require the user to type a trigger for it.
- [x] **Registered-id validation:** a skill may only reference an id registered in the `abilities/` folder. Choose how the editor learns the registered ids: either add a small backend endpoint (e.g. `GET /api/abilities` returning the registered ability ids/names from `AbilityManager`) or reuse the existing skill-summary list. Validate on save that a reference id is registered and surface a clear error otherwise. Do not expand unknown references into inline abilities.
- [x] **Update `web/AGENTS.md`:** the "Referenced Abilities" section currently states the editor has no support and that saving may expand or reject them. Document the new behavior after this change.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/views/SkillEditorPage.vue` (`apiAbilityToForm` lines 343-375, `formAbilityToApi` lines 386-406, `validate` lines 139-167, `enrichFormKeys` lines 275-294, `stripRowKeys` lines 300-311)
  - `web/frontend/src/components/skills/AbilitiesSection.vue` (inline-only `Ability` interface lines 71-92, `emptyAbility` lines 157-180, trigger field lines ~445-454)
  - `web/frontend/src/components/skills/AbilityCard.vue` (read-only card, if reused for reference display)
  - `web/frontend/src/api/client.ts` (add `api.abilities.*` only if the registered-id endpoint is chosen)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/` + `WebServer.java` (only if adding a `GET /api/abilities` endpoint)
  - `web/AGENTS.md`
- **Dependencies:** Backend reference support already landed (serializer commit `99b8513c`; merge engine in `SkillManager.parseAbilities`; registry in `AbilityManager`). No backend save-path change should be needed for isolation — the web layer writes only `skills/{id}.yml`.
- **Constraints:** Per `web/AGENTS.md`, an admin editing a skill must never be able to modify `abilities/<file>.yml` through the skill editor. A referenced ability must not be silently expanded into a full inline copy on save (that would detach the skill from the shared base). Composition API, no `v-html`, `npm run build` must pass.

## Verification & Definition of Done
- [x] `cd web/frontend && npm run build` passes; `./gradlew build` and `./gradlew test` pass if any backend endpoint is added.
- [x] A skill with `- id: "vein_miner"` (no trigger) opens in the editor, shows a "Base/Shared" marker, and saves with a no-op round-trip preserving the bare reference in the skill yml.
- [x] Editing only `unlock_level` of a referenced ability writes `{ id, unlock_level }` to the skill yml and does not touch `abilities/vein_miner.yml`.
- [x] A skill referencing an unregistered id fails validation with a clear error and is not saved.
- [x] Manual check: after Apply & Reload, the skill behaves per the merged definition (base from `abilities/`, overrides from the skill), and other skills referencing the same base are unaffected.

## Issues
- Follow-up scope: full editing of a base ability's inherited fields (dedicated ability editor) is planned separately in ISSUE-275; this ticket only makes references openable, editable-in-place for overrides, and safely round-trippable.
