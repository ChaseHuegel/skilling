# ISSUE-110: Sort skill items in the skills navigation flyout by color then name

**Status:** Open
**Type:** Improvement
**Severity:** Low (UI consistency)

---

## Context & User Story

- **Goal:** As a server admin, I want the skill items in the skills navigation flyout to appear sorted by color then name, just like the skill dashboard page, so that the two views are consistent.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [x] Apply the same color-then-name sort used on the dashboard (`DashboardPage.vue`) to the skills navigation flyout
- [x] Apply the sort before rendering so searching still filters the already-sorted list
- [x] Add an E2E assertion that the flyout order matches the dashboard order for the seeded skills

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/components/layout/SkillPalette.vue` (`filteredSkills` computed, lines 64-70)
  - `web/frontend/src/views/DashboardPage.vue` (reference sort, lines 134-137: `color` then `displayName`/`id`, `localeCompare`)
- **Dependencies:** The dashboard sort at `DashboardPage.vue:134-137` is the reference implementation.
- **Constraints:** Pure presentation change; no API or data changes. Preserve the search/filter behavior.

## Verification & Definition of Done

- [x] `cd web/frontend && npm run build` passes (type-check)
- [x] `cd web/frontend && npm run e2e` passes, including the new ordering assertion
- [x] Runtime check: the navigation flyout orders skills by color then name, matching the dashboard
