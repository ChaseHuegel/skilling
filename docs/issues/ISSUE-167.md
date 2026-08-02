# ISSUE-167: Standardize all lore/description rendering on the escaping helper (no raw `v-html`)

**Status:** Open
**Type:** Improvement
**Severity:** Medium (remaining `v-html` uses violate the documented convention and depend on the escape helper never regressing)

---

## Context & User Story

- **Goal:** As a developer, I want a single safe rendering path for Minecraft color codes so no component can reintroduce an injection risk.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [ ] Replace every `v-html` binding that renders user-authored text with a plain interpolation of the escaped output (or a component wrapper) — target `AbilitiesSection.vue:760` and `SkillTooltip.vue:9,16,19`
- [ ] Keep `renderFormattedText` (`utils/minecraftColors.ts:64-76`) as the single source of truth for color-code rendering, with HTML escaping
- [ ] Remove the now-unused raw-HTML variants in `DisplaySection.vue` (see ISSUE-159) so no divergent reimplementation remains
- [ ] Add a lint rule or build check that forbids `v-html` in the source
- [ ] Add tests asserting escaped rendering for the components above

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/components/skills/AbilitiesSection.vue:760`
  - `web/frontend/src/components/layout/SkillTooltip.vue:9,16,19`
  - `web/frontend/src/utils/minecraftColors.ts:64-76`
  - `web/frontend/src/components/skills/DisplaySection.vue` (ISSUE-159 removal)
- **Dependencies:** ISSUE-159 (DisplaySection XSS fix).
- **Constraints:** `web/AGENTS.md` forbids `v-html`. Color-code rendering must be preserved exactly.

### Root Cause

The remaining `v-html` usages currently route through the escaping helper, so they are safe today — but they violate the documented "No `v-html`" convention and depend on the helper never regressing. A divergent unsafe reimplementation already existed in `DisplaySection` (ISSUE-159).

### Proposed Fix

Consolidate all rendering on `renderFormattedText` output bound via text interpolation, remove `v-html` entirely, and add a guard (lint rule or grep check in CI) against its reintroduction.

## Verification & Definition of Done

- [ ] `cd web/frontend && npm run build` passes
- [ ] Grep shows zero `v-html` occurrences in `web/frontend/src`
- [ ] Tests assert escaped rendering for `AbilitiesSection` and `SkillTooltip`
- [ ] CI includes a `v-html`-forbidding check
