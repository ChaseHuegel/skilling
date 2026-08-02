# ISSUE-166: Remove dead stores/components and split the `AbilitiesSection` monolith

**Status:** Open
**Type:** Improvement
**Severity:** Medium (dead code, duplicated logic, 1,792-line component)

---

## Context & User Story

- **Goal:** As a developer, I want a maintainable frontend without dead stores/components and without a 1,792-line editor component duplicating shared scaffolding.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [x] Remove or wire up the dead modules: `stores/config.ts`, `stores/tags.ts`, `components/common/ToastNotification.vue`, `stores/skills.ts`'s unused methods (`fetch`, `save`, `remove`, `currentSkill`, `fetchList`), `stores/staging.ts`'s `hasFileChanges()`
- [x] Remove dead code in `AbilitiesSection.vue` (`sectionCount()`, `removeAbility()`, the always-false `canDelete`/`canDuplicate` toolbar props)
- [x] Extract the shared mechanic/param/sound/particle scaffolding duplicated with `XpSourcesSection.vue` into a reusable component
- [x] Split `AbilitiesSection.vue` (ability list, lore editor, requirements, mechanics, feedback, on-failure) into focused components
- [x] Make the duplicate-ID check in `SkillEditorPage.vue:147` actually work by populating the skills store (or route it through the API list)
- [x] Enforce `noUnusedLocals`/`noUnusedParameters` in `tsconfig.json` so dead code fails the build
- [x] Keep the built output (`npm run build`) passing throughout

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/components/skills/AbilitiesSection.vue` (whole file; `:220-234`, `:297-301`, `:609-612`)
  - `web/frontend/src/components/skills/XpSourcesSection.vue:118-119`
  - `web/frontend/src/stores/{config,tags,skills,staging}.ts`
  - `web/frontend/src/components/common/ToastNotification.vue`
  - `web/frontend/src/views/SkillEditorPage.vue:147`
  - `web/frontend/tsconfig.json`
- **Dependencies:** `web/AGENTS.md` component conventions.
- **Constraints:** No behavior change for users; pure refactor + dead-code removal. Verify with the E2E suite after the refactor.

### Root Cause

Multiple stores and components are never imported; several page-object methods are dead. `AbilitiesSection.vue` combines ability list, drag-reorder, lore editor + preview, requirements, mechanics + params + filters, feedback, and on-failure reasons, duplicating mechanic/param/sound/particle scaffolding with `XpSourcesSection.vue`. The duplicate-ID validation never runs because the skills store is never populated. `tsconfig.json` has `strict` on but `noUnusedLocals/Parameters` off, so all of this passes silently.

### Proposed Fix

Delete dead modules, extract shared scaffolding into reusable components, split the monolith, wire the duplicate-ID check to a real skill list, and enable the unused-variable checks.

## Verification & Definition of Done

- [x] `cd web/frontend && npm run build` passes with `noUnusedLocals`/`noUnusedParameters` enabled
- [x] No unused imports/components remain (grep-clean)
- [x] `npm run e2e` still passes (no behavior regression)
- [x] Duplicate skill IDs are rejected in the UI
