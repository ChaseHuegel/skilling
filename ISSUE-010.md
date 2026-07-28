# ISSUE-010: Theme Consistency Pass — Dark/Light Mode

**Scope:** Comprehensive visual audit of all pages in both themes.

---

## Known Issues

| Area | Light Mode | Dark Mode |
|------|-----------|-----------|
| **MinecraftIcon slot** | Inventory slot `#1a1a2e` background is too dark against light surface | Correct — dark slot contrasts with dark surface |
| **Skill cards** | Border/shadow visible | Border/shadow washes out — needs darker border or elevated background |
| **Dropdowns/selects** | Correct | White background clashing — needs dark surface color |
| **Tags page chips** | Dark backgrounds visible | OK |
| **Config page inputs** | OK | White inputs on dark — needs dark styling |
| **Filter builder inputs** | OK | Some inputs have light backgrounds |

## Audit Checklist

Every visible component should be checked in both themes:

- [ ] AppTopbar — brand, nav, theme toggle, user area, logout
- [ ] PendingChangesBanner — text, buttons, error pill
- [ ] Dashboard — header, subtitle, skill count badge, skeleton cards
- [ ] Skill cards — border, shadow, hover, ability badge, XP badge
- [ ] MinecraftIcon — slot background, glow ring, fallback circle
- [ ] Login page — card, inputs, button
- [ ] Skill editor — all sections, inputs, selects, buttons
- [ ] Tags page — tag headers, chips, add/remove buttons, inputs
- [ ] Config page — all sections, inputs, checkboxes, buttons
- [ ] FilterBuilder — target/state/tool inputs
- [ ] EvaluatorParameter — type dropdown, dynamic fields
- [ ] SectionToolbar — add/duplicate/delete buttons
- [ ] ToastNotification — success/error/info backgrounds
- [ ] Empty/error states — centered cards

## Fix Strategy

For each issue, apply CSS custom properties from the PrimeVue theme:

```css
/* Instead of hardcoded colors */
background: #fff;                    /* BAD — broken in dark mode */
background: var(--p-surface-section); /* GOOD — adapts to both themes */

border: 1px solid #ddd;              /* BAD */
border: 1px solid var(--p-surface-border); /* GOOD */
```

For elements that need explicit overrides per theme, use the `.app-dark` class:

```css
.some-element {
    background: var(--p-surface-section);
}
.app-dark .some-element {
    background: var(--p-surface-hover);
}
```

## Files to Review

- `src/App.vue`
- `src/components/layout/AppTopbar.vue`
- `src/components/layout/PendingChangesBanner.vue`
- `src/components/common/MinecraftIcon.vue`
- `src/components/common/EvaluatorParameter.vue`
- `src/components/common/FilterBuilder.vue`
- `src/components/common/SectionToolbar.vue`
- `src/components/common/ToastNotification.vue`
- `src/components/skills/SkillCard.vue`
- `src/components/skills/*.vue`
- `src/components/tags/*.vue`
- `src/components/config/ConfigSection.vue`
- `src/views/*.vue`
