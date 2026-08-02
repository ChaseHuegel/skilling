# ISSUE-161: Never navigate away when a save fails (SkillEditor, Config, Tags, GuiLayout)

**Status:** Closed
**Type:** Bug
**Severity:** High (silent edit loss on save failure)

---

## Context & User Story

- **Goal:** As an admin, I want to be told when my changes could not be saved, and to stay on the page so my work is not lost.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [x] In `SkillEditorPage.vue:390-420`, only navigate on a successful save; on failure show an error and stay put
- [x] Apply the same rule to `ConfigPage.vue:158-167`, `TagsPage.vue:139-148`, and `GuiLayoutPage.vue:192-200`
- [x] Ensure the cancel/discard flows do not mask a failed save
- [x] Add E2E/unit coverage: a failed save keeps the user on the page with the unsaved state and an error message

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/views/SkillEditorPage.vue:390-420`
  - `web/frontend/src/views/ConfigPage.vue:158-167`
  - `web/frontend/src/views/TagsPage.vue:139-148`
  - `web/frontend/src/views/GuiLayoutPage.vue:192-200`
- **Dependencies:** `api/client.ts` error propagation.
- **Constraints:** Do not trap the user; navigation on success and explicit user cancel remain.

### Root Cause

Every route-leave save swallows errors (`catch { /* navigate anyway */ }`) and proceeds with navigation, silently discarding all edits with no user feedback. The same pattern exists in the cancel-discard flows.

### Proposed Fix

Await the save; on rejection, keep the route, surface a clear error, and leave the dirty state intact. Only navigate after a resolved save or an explicit user cancel.

### Resolution

Each route-leave `leaveSave` now awaits the save inside its try, and only calls `pendingNavigation()` on success. On rejection it surfaces the error via the page's error banner, clears the `saving` flag, and re-opens the unsaved-changes dialog so the admin can retry or explicitly discard — the dirty form state is left untouched. The explicit `leaveDiscard`/cancel paths are unchanged (user-initiated discard remains allowed). A new E2E test forces the skill PUT to fail via a route abort, triggers the leave dialog, clicks "Save & Leave", and asserts the user stays on the editor with the edited value intact and an error banner shown.

## Verification & Definition of Done

- [x] `cd web/frontend && npm run build` passes
- [x] E2E/unit test: forced save failure leaves the user on the page with edits intact and an error shown (new E2E test; full suite 75/75 pass)
- [x] Manual smoke: stop the API, edit, save → error shown, no navigation (covered by the route-abort E2E test)
