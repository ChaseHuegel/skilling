# ISSUE-016: Dark/Light Mode Theme Gaps — PrimeVue Init, Input Unification, Contrast Fixes

**Iteration** — Estimated: 2-3 days

---

## Overview

Previous theme work (ISSUE-010) replaced hardcoded colors with CSS
custom properties, but several systemic issues remain:

1. **PrimeVue dark mode timing** — The `.app-dark` class is applied
   *after* PrimeVue initializes, so CSS variables aren't set correctly
   in scoped components. This causes white cards in dark mode, invisible
   text, and wrong border colors.

2. **Fragmented input styling** — Config page uses raw `<input>` while
   the skill editor uses `.field-input` classes. No shared component,
   so they render differently (white boxes vs transparent).

3. **Critical contrast failures** — Cancel button, tag pills, duplicate
   button, section borders all fail WCAG contrast in one theme or the
   other.

---

## Phase A: PrimeVue Dark Mode Initialization Fix

### File

**`web/frontend/src/main.ts`**

### Change

Move the dark mode class application to *before* `app.use(PrimeVue, ...)`
so the theme plugin reads the correct initial state:

```typescript
// Before app.use(PrimeVue...)
const prefersDark = localStorage.getItem('skilling_dark_mode') === 'true';
if (prefersDark) {
    document.documentElement.classList.add('app-dark');
}
```

This ensures PrimeVue's `darkModeSelector: '.app-dark'` check at plugin
init time sees the correct class and sets `data-p-theme="dark"` on the
document, which in turn makes all `var(--p-*)` resolve to dark values.

### Why This Matters

PrimeVue's theme plugin, when given `darkModeSelector: '.app-dark'`,
does a `document.querySelector('.app-dark')` at plugin setup time. If
the class isn't present then, but is added later (via `App.vue`'s
`onMounted`), the theme plugin never sees it and CSS variables remain
at their light-mode defaults. Scoped components that reference
`var(--p-surface-section)` get the light value, causing white cards
with white text in dark mode.

---

## Phase B: AppInput Component

### Files

- **Create:** `web/frontend/src/components/common/AppInput.vue`
- **Update:** `src/views/ConfigPage.vue`, `src/views/TagsPage.vue`,
  `src/components/skills/SkillIdentitySection.vue`,
  `src/components/skills/DisplaySection.vue`,
  `src/components/skills/ProgressionSection.vue`,
  `src/components/skills/XpSourcesSection.vue`,
  `src/components/skills/AbilitiesSection.vue`,
  `src/components/tags/MaterialMultiSelect.vue`,

### AppInput.vue API

```html
<AppInput v-model="value" type="text" label="My Field" />
<AppInput v-model="checked" type="checkbox" label="Enable" />
<AppInput v-model="num" type="number" label="Count" min="1" max="100" />
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `modelValue` | `string \| number \| boolean` | — | v-model |
| `type` | `'text' \| 'number' \| 'checkbox' \| 'password'` | `'text'` | Input type |
| `label` | `string` | `''` | Label text |
| `placeholder` | `string` | `''` | Placeholder |
| `min` / `max` | `number` | — | For number type |
| `step` | `number` | — | For number type |

**Checkbox type** renders a custom styled checkbox (not native) using
a CSS-only approach: a 16×16 rounded square with `var(--p-primary-color)`
background when checked, with a white SVG checkmark. Borders use
`var(--p-surface-border)` in unchecked state.

**Text/number type** uses `var(--p-surface-input)` background,
`var(--p-surface-border)` border, `var(--p-text-color)` text, with a
`2px` focus ring using `var(--p-primary-color)` (via `:focus-visible`).

---

## Phase C: Critical Contrast Fixes

### Files

- `src/views/DashboardPage.vue`
- `src/components/skills/SkillCard.vue`
- `src/components/tags/MaterialMultiSelect.vue`
- `src/components/common/SectionToolbar.vue`
- `src/views/SkillEditorPage.vue`

### Fixes

| Issue | Fix |
|-------|-----|
| **White card in dark mode** | Ensure `.skill-card` uses `var(--p-surface-section)` with NO fallback value. Remove `#fff` fallbacks from all `var(--p-*)` references in scoped styles |
| **Cancel button dark mode** | Add `.app-dark .btn-secondary { background: var(--p-surface-hover); color: var(--p-text-color); }` |
| **Tag pill light mode** | Chip background uses `color-mix(in srgb, var(--p-surface-border) 50%, var(--p-surface-section))` to adapt in both modes |
| **Duplicate button dark mode** | Use a more opaque `color-mix` with the primary color to ensure contrast |
| **Section borders dark mode** | Replace `var(--p-surface-border)` with `color-mix(in srgb, var(--p-surface-border) 50%, black)` in dark mode only |
| **Header nav active** | `.nav-link.router-link-exact-active` in dark mode: increase background opacity |

---

## Phase D: Final Theme Audit

### File

Run across `src/`:

```bash
grep -rnE "#[0-9a-fA-F]{6}" src/ | grep -viE "var\(--|#fff|#000" | grep -iE "color|background|border"
```

Fix any remaining hardcoded colors that don't use CSS variables.

---

## Commit Plan

| # | Phase | Message |
|---|-------|---------|
| 1 | A | `fix(web): initialize PrimeVue dark mode before plugin loads` |
| 2 | B | `feat(web): create AppInput component with custom checkbox, unify form inputs` |
| 3 | C | `fix(web): critical contrast fixes for both themes` |
| 4 | D | `chore(web): final theme audit — remove remaining hardcoded colors` |
