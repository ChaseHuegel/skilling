# ISSUE-226: Web ability editor on_failure reason list is incomplete

## Context & User Story
- **Goal:** As a server admin, I want to add on-failure feedback for every failure reason the engine can produce, including `exhaustion`.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements
- [ ] Align `FAILURE_REASON_OPTIONS` in `OnFailureEditor.vue:23` with the engine's `FailureReason` enum (`FailureReason.java`): add `exhaustion` (already used by `mining.yml` and the template) and the remaining emitted reasons so the dropdown covers everything the engine can dispatch.
- [ ] The on_failure YAML keys are matched by `check.failureReason().name().toLowerCase()` (`SkillEventListener.java:534`), so the dropdown options must be the lowercase snake_case forms the engine looks up (`cooldown`, `missing_item`, `missing_state`, `exhaustion`, and, if meaningful, `insufficient_items`, `unknown`).
- [ ] Add friendly labels for the new options and keep existing entries (even unrecognized keys from hand-authored YAML) rendering as-is rather than being lost.
- [ ] Add an E2E or component-level regression test: an ability with an `exhaustion` on-failure entry renders and is preserved on save.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/components/skills/OnFailureEditor.vue`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (key lookup reference)
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/requirements/FailureReason.java`
- **Dependencies:** none.
- **Constraints:** Existing on-failure entries are stored in a map and already survive round-trips; this ticket only extends the dropdown. Confirm which reasons are actually dispatched before adding each option.

## Verification & Definition of Done
- [ ] The dropdown offers the engine's full set of dispatachable reasons (at minimum including `exhaustion`).
- [ ] An `exhaustion` on-failure entry added in the GUI survives save and reload.
- [ ] `cd web/frontend && npm run build` passes.
- [ ] Relevant E2E/component tests pass.
