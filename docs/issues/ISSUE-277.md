# ISSUE-277: E2E — Fix Remaining Pre-Existing Flakes (Save-Replace Dirty Dialog and Mobile Lore)

## Context & User Story
- **Goal:** As a maintainer, I want the full Playwright suite to pass reliably. Two pre-existing failures remain after ISSUE-258 fixed the auth redirect and topbar overflow: the staged-changes "discard removes pending changes" test trips the skill editor's "Unsaved changes" leave dialog on a save-and-reload flow, and the skill-level lore round-trip test intermittently fails on the mobile viewport because the Save Changes banner never appears.
- **Agent Role:** You are an expert frontend and QA engineer executing this task. These failures are not regressions from the reference-ability or tags work; they reproduce on a clean baseline and are separate from ISSUE-258's named failures.

## Implementation Requirements
- [x] **Staged-changes discard race:** `staged-changes.spec.ts` beforeEach saves a skill (PUT + `window.location.reload()`), then the test navigates to the dashboard and clicks the pending-changes Discard button. On the reloaded editor page the "Unsaved changes" leave dialog can be open, so its modal overlay intercepts the banner's Discard click. Make the editor's post-save reload land in a clean (not dirty) state, or make the test wait for the reload and the dialog to clear before clicking Discard.
- [x] **Mobile lore round-trip flake:** `skill-editor.spec.ts` "skill-level lore lines persist through save, apply, and reload" intermittently fails on the 375x667 mobile viewport because after Apply & Reload the `Save Changes` button is never visible when the test saves an unrelated edit. Make the save flow robust on the narrow viewport (the banner or an intervening dialog may be hidden/off-screen) or make the test wait deterministically.
- [x] Re-run `cd web/frontend && npm run e2e` and confirm the full suite passes with no retries required.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/views/SkillEditorPage.vue` (`save()` calls `window.location.reload()`; `onBeforeRouteLeave` shows the leave dialog when `isDirty`)
  - `web/frontend/e2e/specs/staged-changes.spec.ts` (beforeEach save + test Discard)
  - `web/frontend/e2e/specs/skill-editor.spec.ts` (lines ~211-260, lore round-trip; mobile viewport)
  - `web/frontend/e2e/pages/DashboardPage.ts` (`discardChanges`, `applyChanges`)
- **Dependencies:** ISSUE-258 fixed the auth redirect and topbar overflow; these two failures remained afterward and are tracked here as a follow-up.
- **Constraints:** Per `web/AGENTS.md`, use the Composition API, keep the build free of dead code, and no `v-html`. The Playwright suite is order-independent and runs serially with per-test staging resets. Use `expect.poll` / retrying assertions instead of fixed sleeps.

## Verification & Definition of Done
- [x] `cd web/frontend && npm run build` passes.
- [x] `staged-changes.spec.ts` passes on desktop and mobile repeatedly.
- [x] `skill-editor.spec.ts` passes on desktop and mobile repeatedly (no retries).
- [x] `cd web/frontend && npm run e2e` passes when the dev server is available.

## Resolution

Both flakes shared one root cause: the skill editor treated the initial (empty) form as dirty until the async skill fetch completed, so the save/apply reload window could open the "Unsaved changes" leave dialog or flash the Save Changes banner, and the tests raced the app's `window.location.reload()` which runs in the microtask after the HTTP response.

- `SkillEditorPage.vue`: baseline `cleanForm` at the start of `onMounted` so the loading phase is never dirty (no leave dialog, no banner flash).
- `SkillEditorPage.ts` / `DashboardPage.ts`: `save()` and `applyChanges()` now subscribe to the next `load` event before clicking, so the test waits for the reload to actually commit instead of only for the HTTP response.
- `staged-changes.spec.ts`: wait for the reloaded editor in `beforeEach` before navigating to the dashboard.
- `skill-editor.spec.ts`: the lore round-trip edit is now derived from the current display name (toggling an ` E2E` suffix) instead of a fixed string. The desktop and mobile projects share one server, so a fixed rename that the desktop project applied made the mobile run's `setDisplayName` a no-op, leaving the form clean and the Save Changes button invisible.
