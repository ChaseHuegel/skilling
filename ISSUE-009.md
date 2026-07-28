# ISSUE-009: Navigation, Banner & Topbar Polish

**Iteration** — Estimated: 1 day

---

## Overview

Following the dashboard visual redesign (ISSUE-008), this issue polishes
the remaining shared UI elements: the top navigation bar and the pending
changes banner. These components appear on every page and have a
disproportionate impact on the overall feel of the GUI.

---

## What's Changed vs ISSUE-008

The original ISSUE-008 plan included P0.3 (active nav link) and P2.12
(theme toggle polish) as stretch goals. Those were deferred to avoid
scope creep. This issue picks them up alongside the banner improvements
that were always in the review notes but never formally planned.

---

## Phase A: Active Nav Link + AppTopbar Polish

### File

**`web/frontend/src/components/layout/AppTopbar.vue`**

### Changes

| Area | Before | After |
|------|--------|-------|
| **Active link** | All nav links styled identically | Active route gets `var(--p-primary-color)` text + subtle background |
| **Theme toggle** | Emoji `🌙`/`☀️` characters | Inline SVG sun/moon icons, swapped via `v-if`/`v-else` |
| **User display** | Plain `{{ authStore.user }}` text | Small person SVG icon + username in muted style |
| **Logout button** | Bordered button with hover background | Text link with underline-on-hover — visually quieter |
| **Brand** | Text-only "Skilling" | Small pickaxe SVG icon next to the brand text |

### CSS Details

```css
/* Active link */
.nav-link.router-link-active {
    color: var(--p-primary-color);
    background: color-mix(in srgb, var(--p-primary-color) 10%, transparent);
}
.nav-link.router-link-exact-active {
    color: var(--p-primary-color);
    font-weight: 600;
}

/* Theme toggle SVG */
.theme-toggle svg {
    width: 18px;
    height: 18px;
    display: block;
}
.theme-toggle {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.25rem;
    border-radius: 4px;
    color: var(--p-text-muted-color);
}
.theme-toggle:hover {
    background: var(--p-surface-hover);
    color: var(--p-text-color);
}
```

---

## Phase B: PendingChangesBanner Redesign

### File

**`web/frontend/src/components/layout/PendingChangesBanner.vue`**

### Changes

| Area | Before | After |
|------|--------|-------|
| **Text** | `"⚠️ 3 file(s) have pending changes."` | `"3 files changed"` with small edit SVG icon. The warning emoji is redundant with the yellow background |
| **Button hierarchy** | Apply & Reload and Discard look equally important | Apply = filled primary button (high prominence), Discard = small text link (low prominence) |
| **Error display** | Error text as block element below buttons, shifts layout | Inline error pill next to buttons, doesn't shift layout |
| **Layout** | Simple flex row | `space-between` on mobile to prevent overflow, gap on desktop |

### Visual Structure

```
┌──────────────────────────────────────────────────────────┐
│  ✏️ 3 files changed   [Apply & Reload]  Discard          │
│                                         (error pill)     │
└──────────────────────────────────────────────────────────┘
```

---

## Commit Plan

| # | Phase | Message |
|---|-------|---------|
| 1 | A | `feat(web): active nav link highlighting, SVG theme toggle, AppTopbar polish` |
| 2 | B | `feat(web): PendingChangesBanner redesign — text, button hierarchy, inline error` |

Each commit verified with `npm run build` + full Playwright E2E suite.
