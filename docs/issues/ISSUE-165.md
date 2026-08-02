# ISSUE-165: Replace index-keyed `v-for` in reorderable lists with stable IDs

**Status:** Open
**Type:** Improvement
**Severity:** Medium (stale per-row UI state and lost input focus after drag reorder)

---

## Context & User Story

- **Goal:** As an admin, I want dragging an ability or XP source to reorder it without the expanded/focus state attaching to the wrong row or my input caret jumping.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [ ] Use stable IDs (ability/source id, or a stable generated key) instead of `:key="idx"`/`sectionExpanded["{idx}-{key}"]` in reorderable lists
- [ ] Migrate: `AbilitiesSection.vue:622`, `XpSourcesSection.vue:129`, `EvaluatorParameter.vue:137`, `FilterBuilder.vue:48`, `PageTabs.vue:5`, `LevelUpCommandsSection.vue:6`, `DisplaySection.vue:144`
- [ ] Ensure expanded/dragging per-row state tracks the row identity, not its position
- [ ] Add a test: reorder rows → expanded state and input focus follow the correct item

## Technical Specifications & Context

- **Target Files:** `web/frontend/src/components/skills/AbilitiesSection.vue:622`, `web/frontend/src/components/skills/XpSourcesSection.vue:129`, `web/frontend/src/components/common/EvaluatorParameter.vue:137`, `web/frontend/src/components/common/FilterBuilder.vue:48`, `web/frontend/src/components/layout/PageTabs.vue:5`, `web/frontend/src/components/skills/LevelUpCommandsSection.vue:6`, `web/frontend/src/components/skills/DisplaySection.vue:144`
- **Dependencies:** `useDragReorder.ts:10-18` reorders on every dragover.
- **Constraints:** New keys must be stable across reorder (do not regenerate per render).

### Root Cause

Index keys force DOM reuse that loses input focus and cause per-index state (expanded, drag) to attach to the wrong rows after a drag reorder, since `useDragReorder` reorders the array on every dragover.

### Proposed Fix

Key each row by a stable identity (e.g. the ability id or a persistent client-generated UUID assigned at creation) and key expanded/drag state by that identity.

## Verification & Definition of Done

- [ ] `cd web/frontend && npm run build` passes
- [ ] E2E/manual test: expand an ability, reorder it, expanded state stays on the same ability
- [ ] Manual smoke: editing mid-string in a reordered list no longer jumps the caret
