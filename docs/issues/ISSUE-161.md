# ISSUE-161: Never navigate away when a save fails (SkillEditor, Config, Tags, GuiLayout)

**Status:** Open
**Type:** Bug
**Severity:** High (silent edit loss on save failure)

---

## Context & User Story

- **Goal:** As an admin, I want to be told when my changes could not be saved, and to stay on the page so my work is not lost.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [ ] In `SkillEditorPage.vue:390-420`, only navigate on a successful save; on failure show an error and stay put
- [ ] Apply the same rule to `ConfigPage.vue:158-167`, `TagsPage.vue:139-148`, and `GuiLayoutPage.vue:192-200`
- [ ] Ensure the cancel/discard flows do not mask a failed save
- [ ] Add E2E/unit coverage: a failed save keeps the user on the page with the unsaved state and an error message

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

## Verification & Definition of Done

- [ ] `cd web/frontend && npm run build` passes
- [ ] E2E/unit test: forced save failure leaves the user on the page with edits intact and an error shown
- [ ] Manual smoke: stop the API, edit, save → error shown, no navigation
