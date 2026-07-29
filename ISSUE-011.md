# ISSUE-011: Web Frontend Visual Design Review

## Description

Comprehensive review of the Skilling web frontend visual design against
comparable admin GUIs. Identifies gaps and improvement areas. No code
changes are made — this is a planning document only.

## Commendable Design Decisions

- PrimeVue Aura theme provides a solid, themeable base
- Skeleton loading states with shimmer animation on key views
- Card entrance animations with staggered delays
- `color-mix()` for dynamic skill-color theming (badges, borders, highlights)
- MinecraftIcon CDN fallback chain (shimmer → letter fallback → texture)
- Consistent modal pattern across all views
- Leave-guard dialogs on dirty forms
- Sticky save banners for unsaved changes
- No PrimeVue components used directly — all custom HTML with scoped CSS

## Critical Issues

### 1. Mobile Responsiveness

| Issue | Recommendation |
|-------|---------------|
| SkillEditorPage field rows will break on narrow screens | Add `@media (max-width: 640px)` to stack labels and inputs vertically |
| Topbar nav links overflow on mobile | Implement hamburger menu or responsive nav collapse |
| No mobile-first breakpoints in many components | Audit all views for <640px viewport behavior |

### 2. Accessibility

| Issue | Recommendation |
|-------|---------------|
| No skip-to-content link | Add `<a href="#main-content">` skip link |
| Icon-only buttons lack `aria-label` | Add `aria-label` to all icon-only buttons (duplicate, delete, close) |
| Modal dialogs lack focus trapping | Implement focus trap with `keydown` listener and `aria-modal="true"` |
| Drag-and-drop is not keyboard accessible | Provide alternative reorder buttons (↑↓) or use `aria-grabbed` pattern |
| No `aria-live` regions | Add to filtered lists, error banners, toast notifications |
| No `<form>` elements | Wrap login and data entry in `<form>` for accessibility and autofill |
| No `prefers-reduced-motion` query | Disable animations for users who prefer reduced motion |

### 3. Typography & Consistency

| Issue | Recommendation |
|-------|---------------|
| ~12 unique font sizes used ad-hoc | Define a type scale using CSS custom properties |
| No global type scale | Create `--text-xs, --text-sm, --text-base, --text-lg, --text-xl` |
| Heading hierarchy inconsistent across views | Ensure each page has exactly one `<h1>`, proper `<h2>`+ nesting |
| SkillEditorPage has no `<h1>` | Add page title with skill name |

### 4. Navigation & Routing

| Issue | Recommendation |
|-------|---------------|
| No 404/not-found route | Add catch-all route with helpful message |
| No breadcrumb navigation | Add breadcrumbs for deep views (SkillEditor) |
| No back button in SkillEditor | Add explicit "Back to Dashboard" link |
| No route-level loading indicator | Add top progress bar during route transitions |

### 5. Form Design

| Issue | Recommendation |
|-------|---------------|
| Login form missing `<form>` element | Wrap in `<form>` with `@submit.prevent` |
| No field-level validation messages | Add per-field error styling and messages |
| No required field indicators | Add visual markers for required fields |
| TagsPage missing empty state | Add "No tags defined" message when tags object is empty |
| No clear button on search inputs | Add "×" clear button or Escape-to-clear |

### 6. Feedback & Error Handling

| Issue | Recommendation |
|-------|---------------|
| Save actions use `window.location.reload()` | Replace with proper state refresh where possible |
| No offline/network error state | Add global connection-lost banner |
| No retry button on Tags/Config load errors | Add retry CTA like Dashboard and AbilitiesPage |
| Toast colors use hardcoded hex | Use `var(--p-green-600)` etc. for theme coherence |

## Implementation Priority

1. Mobile responsiveness (SkillEditorPage & Topbar) — **High**
2. `<form>` elements for accessibility — **High**
3. Icon-button `aria-label` — **High**
4. Modal focus trapping — **High**
5. 404 route — **Medium**
6. Type scale definition — **Medium**
7. Field-level validation — **Medium**
8. Drag-reorder keyboard alternative — **Medium**
9. Reduced motion support — **Low**
10. Breadcrumb navigation — **Low**

Risk: Very Low. Document only; no changes implemented.
