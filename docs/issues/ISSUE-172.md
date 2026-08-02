# ISSUE-172: Follow-up to ISSUE-110 — sort the actual navigation flyout (topbar dropdown) by color then name

**Status:** Open
**Type:** Bug
**Severity:** Medium (ISSUE-110 was marked done but its goal was never achieved — the wrong component was sorted)

---

## Context & User Story

- **Goal:** As a server admin, I want the skills listed in the **navigation menu's "Skills" dropdown** (topbar flyout) ordered by color then name, matching the dashboard. This was the original goal of ISSUE-110, which is still not met.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [ ] Sort the `skills` list used by `AppTopbar.vue`'s flyout by color then name (same comparator as the dashboard) before it is rendered
- [ ] Apply the sort so the dropdown renders sorted regardless of the API's raw response order (sort in `onMounted` after `api.skills.list()`, or expose a sorted computed used by the `v-for`)
- [ ] Reuse the dashboard comparator (`DashboardPage.vue:134-137`) — preferably extract it into a shared helper since it now exists in three places (DashboardPage, SkillPalette, and here)
- [ ] Add/replace an E2E assertion that targets the **topbar flyout** (`.dropdown-item` in `AppTopbar.vue`) against the dashboard order — the existing assertion in `gui-layout.spec.ts` tests the layout **palette** (`.palette-item`), not the flyout, so it never caught this
- [ ] Update ISSUE-110's status note (it was incorrectly marked complete; see below)

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/components/layout/AppTopbar.vue:16` (unsorted `v-for="s in skills"`) and `:64-70` (`skills.value = await api.skills.list()` with no sort)
  - `web/frontend/src/views/DashboardPage.vue:134-137` (reference sort: `color` then `displayName`/`id`, `localeCompare`)
  - `web/frontend/src/components/layout/SkillPalette.vue:64-77` (already sorted — this is the **GUI Layout palette**, NOT the navigation flyout)
  - `web/frontend/e2e/specs/gui-layout.spec.ts` (the "palette skills sort" test asserts `.palette-item`, the wrong component)
- **Dependencies:** ISSUE-110 (original attempt, `6a6438e`). The existing E2E assertion in `gui-layout.spec.ts` will need to be corrected or extended to assert the topbar flyout.
- **Constraints:** Pure presentation change; no API or data changes. Preserve existing nav/filter behavior.

### Root Cause

ISSUE-110's fix sorted `SkillPalette.vue`'s `filteredSkills` computed, but `SkillPalette` is the **GUI Layout page's palette** (rendered at `GuiLayoutPage.vue:47` for placing skills into chest slots) — not the skills navigation flyout. The actual flyout lives in `AppTopbar.vue`, which renders `api.skills.list()` results directly with no ordering. The E2E assertion added in the same commit compared the palette order to the dashboard order, so it passed while the real navigation flyout stayed in API order.

### Proposed Fix

Sort the topbar's `skills` array (in `onMounted`, or via a computed) with the same color-then-name comparator used by the dashboard. Extract the comparator into a shared utility (e.g. `utils/skillSort.ts`) and use it in `DashboardPage.vue`, `SkillPalette.vue`, and `AppTopbar.vue` so the three views can never diverge again. Add an E2E assertion that opens the topbar dropdown and compares `.dropdown-item` order to the dashboard order.

## Verification & Definition of Done

- [ ] `cd web/frontend && npm run build` passes (type-check)
- [ ] E2E: a new/updated assertion verifies the **topbar flyout** (`.dropdown-item`) order matches the dashboard order for the seeded skills
- [ ] The existing `gui-layout.spec.ts` palette assertion still passes (palette behavior unchanged)
- [ ] Runtime check: hovering the "Skills" nav link shows the flyout ordered by color then name, matching the dashboard
- [ ] ISSUE-110's index bullet and ticket note the follow-up / corrected status
