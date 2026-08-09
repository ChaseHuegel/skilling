# ISSUE-277: E2E — Fix Remaining Pre-Existing Flakes (Save-Replace Dirty Dialog and Mobile Lore)

## Context & User Story
- **Goal:** As a maintainer, I want the full Playwright suite to pass reliably. Two pre-existing failures remain after ISSUE-258 fixed the auth redirect and topbar overflow: the staged-changes "discard removes pending changes" test trips the skill editor's "Unsaved changes" leave dialog on a save-and-reload flow, and the skill-level lore round-trip test intermittently fails on the mobile viewport because the Save Changes banner never appears.
- **Agent Role:** You are an expert frontend and QA engineer executing this task. These failures are not regressions from the reference-ability or tags work; they reproduce on a clean baseline and are separate from ISSUE-258's named failures.

## Implementation Requirements
- [ ] **Staged-changes discard race:** `staged-changes.spec.ts` beforeEach saves a skill (PUT + `window.location.reload()`), then the test navigates to the dashboard and clicks the pending-changes Discard button. On the reloaded editor page the "Unsaved changes" leave dialog can be open, so its modal overlay intercepts the banner's Discard click. Make the editor's post-save reload land in a clean (not dirty) state, or make the test wait for the reload and the dialog to clear before clicking Discard.
- [ ] **Mobile lore round-trip flake:** `skill-editor.spec.ts` "skill-level lore lines persist through save, apply, and reload" intermittently fails on the 375x667 mobile viewport because after Apply & Reload the `Save Changes` button is never visible when the test saves an unrelated edit. Make the save flow robust on the narrow viewport (the banner or an intervening dialog may be hidden/off-screen) or make the test wait deterministically.
- [ ] Re-run `cd web/frontend && npm run e2e` and confirm the full suite passes with no retries required.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/views/SkillEditorPage.vue` (`save()` calls `window.location.reload()`; `onBeforeRouteLeave` shows the leave dialog when `isDirty`)
  - `web/frontend/e2e/specs/staged-changes.spec.ts` (beforeEach save + test Discard)
  - `web/frontend/e2e/specs/skill-editor.spec.ts` (lines ~211-260, lore round-trip; mobile viewport)
  - `web/frontend/e2e/pages/DashboardPage.ts` (`discardChanges`, `applyChanges`)
- **Dependencies:** ISSUE-258 fixed the auth redirect and topbar overflow; these two failures remained afterward and are tracked here as a follow-up.
- **Constraints:** Per `web/AGENTS.md`, use the Composition API, keep the build free of dead code, and no `v-html`. The Playwright suite is order-independent and runs serially with per-test staging resets. Use `expect.poll` / retrying assertions instead of fixed sleeps.

## Verification & Definition of Done
- [ ] `cd web/frontend && npm run build` passes.
- [ ] `staged-changes.spec.ts` passes on desktop and mobile repeatedly.
- [ ] `skill-editor.spec.ts` passes on desktop and mobile repeatedly (no retries).
- [ ] `cd web/frontend && npm run e2e` passes when the dev server is available.
