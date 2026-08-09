# ISSUE-275: Research — Ability Editor and List Page for Standalone Base/Shared Abilities

## Context & User Story
- **Goal:** As a server admin, I want a dedicated web page to add, edit, and delete base/shared abilities that live in standalone yml files under `abilities/`. I want it to function like the skills editor page, but stay distinct from the existing `/abilities` page, which only searches read-only abilities across all skills.
- **Agent Role:** You are an expert frontend and backend architect producing a research report. This is a research ticket: deliver a development plan and proposal in a markdown report document for the maintainer to review. Do not implement production code.
- **Deliverable:** `docs/reports/REPORT_ABILITY-EDITOR.md`, written to the standards in `docs/AGENTS.md` (header states the owning ticket and date, cross-links to this ticket, ASD-STE100 prose).

## Research Goals
- [ ] Map the current state precisely: the read-only `/abilities` search page (`AbilitiesPage.vue`), the inline ability editor built into the skill editor (`SkillEditorPage.vue` + `AbilitiesSection.vue`), the `abilities/` data-folder loader and merge engine (`AbilityManager`, `SkillManager.parseAbilities`), and the web API surface (`WebServer.java` routes, `SkillHandler`, `SkillSerializer`).
- [ ] Define the scope of the new page: a standalone-ability list page and a standalone-ability editor page, distinct from the existing `/abilities` search page. Decide the route names, topbar navigation, and whether the editor reuses `AbilitiesSection.vue` or needs a separate component.
- [ ] Plan the backend API: proposed `AbilityHandler` and routes (`GET/POST/PUT/DELETE /api/abilities[/{id}]` or equivalent), the ability DTO/serializer (note `SkillSerializer.parseAbility`/`abilityToMap` are private and skill-DTO-bound — propose reuse, extraction, or new code), id validation (`[a-z_][a-z0-9_]*`), fail-fast validation of the yml against the live registries (model on `SkillHandler.validateStagedSkill`), and `StagingManager` support for `abilities/*.yml` (stage/create/delete, apply, conflict detection, backups).
- [ ] Plan the frontend: `api.abilities.*` functions in `client.ts`, list view (reuse `AbilityCard.vue` where sensible), editor view, and route registration in `router.ts`.
- [ ] Analyze the merge/interaction implications: editing a base ability affects every skill that references it. Propose how the UI should surface this (affected-skills awareness, warnings on edit/delete, or explicit confirm).
- [ ] Coordinate with ISSUE-274: that ticket teaches the skill editor to reference base abilities safely; this page is the dedicated home for editing the shared definitions. Cross-reference and propose a sequencing/dependency order.
- [ ] Produce a phased development plan with a recommended architecture, an alternative considered, and open questions for the maintainer to decide.

## Technical Specifications & Context
- **Target Files (for research, not modification):**
  - `web/frontend/src/views/AbilitiesPage.vue` (existing read-only search page, must stay distinct)
  - `web/frontend/src/views/SkillEditorPage.vue`, `web/frontend/src/components/skills/AbilitiesSection.vue`, `web/frontend/src/components/skills/AbilityCard.vue`
  - `web/frontend/src/api/client.ts`, `web/frontend/src/router.ts`, `web/frontend/src/components/layout/AppTopbar.vue`
  - `src/main/java/io/github/chasehuegel/skilling/engine/AbilityManager.java`, `engine/SkillManager.java` (`parseAbilities` merge)
  - `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java`, `web/handler/SkillHandler.java`, `web/dto/SkillSerializer.java`, `web/staging/StagingManager.java`
  - `src/main/resources/abilities/vein_miner.yml` (schema example), `docs/users/creating-skills.md` (Reusable Abilities section), `docs/dev/template-skill.yml`
- **Dependencies:** ISSUE-274 (skill-editor reference support) is the natural prerequisite for a shared-ability workflow; the report must propose whether to sequence after it.
- **Constraints:** No production code changes in this ticket. The report lands in `docs/reports/`. Research freely (existing patterns in this codebase, Paper APIs); do not guess.

## Verification & Definition of Done
- [ ] `docs/reports/REPORT_ABILITY-EDITOR.md` exists, states the owning ticket and date in its header, and cross-links to this ticket.
- [ ] The report includes: current-state analysis, proposed API surface, proposed frontend structure, staging/validation plan, the shared-edit interaction analysis, a phased implementation plan, an alternative approach, and explicit open questions.
- [ ] No production files were changed by this ticket.
