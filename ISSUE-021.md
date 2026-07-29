# ISSUE-021: Visual Hierarchy Improvements for Ability Editor Cards

**Scope:** Improve visual hierarchy in the skill editor's ability editor cards
by adding color-coded section gutters, differentiated inner cards, depth
layering via background tints, per-section collapse, and optional section
icons.

---

## Background

The ability editor (`AbilitiesSection.vue`) is a 1585-line monolithic
component. Each ability card contains 5 major sections (Lore Lines,
Requirements, Mechanics, Feedback, On Failure), each of which contains
nested sub-sections and inner cards. Currently, all sections are separated
only by a thin `border-top`, and all inner card types (item, mechanic,
particle, sound, failure, param) share identical styling. This makes it
difficult to visually parse the hierarchy at a glance, especially for
complex abilities with many mechanics and feedback entries.

## Implementation Plan (5 Commits)

Each commit is self-contained and independently revertible.

| # | Commit | Description | Risk |
|---|--------|-------------|------|
| 1 | Color-coded section gutters | Colored left borders + tinted backgrounds per section | Low |
| 2 | Differentiated inner cards | Distinct left accent bars per card type | Low |
| 3 | Depth layering via tint | Background tint shifts at each nesting level | Low |
| 4 | Per-section collapse | Collapsible sub-sections within ability cards | Medium |
| 5 | Section SVG icons | Inline SVG icons per section label | Low |

**Note:** The icon commit (5) is intentionally last so it can be easily
reverted by running `git revert HEAD` without affecting the other four
improvements.

---

### Requirements

#### Commit 1 — Color-Coded Section Gutters

**Goal:** Give each major `section-block` a distinct visual identity via
colored left border + subtle tinted background.

Color scheme:

| Section | CSS Variable | Hex (Aura dark) | Usage |
|---------|-------------|-----------------|-------|
| Lore Lines | `--p-cyan-400` | `#22d3ee` | Left border, label accent |
| Requirements | `--p-orange-400` | `#fb923c` | Left border, label accent |
| Mechanics | `--p-purple-400` | `#c084fc` | Left border, label accent |
| Feedback | `--p-green-400` | `#4ade80` | Left border, label accent |
| On Failure | `--p-red-400` | `#f87171` | Left border, label accent |

**Behavior:**
- Each `section-block` gets a 4px `border-left` in its accent color and
  padding-left increase (0.75rem → 1rem) to offset from the border
- Each section-block gets a `background` using `color-mix(in srgb, <color> 4%, transparent)`
- Section labels inherit the accent color instead of default text color
- border-top on section-block is preserved but reduced to 1px solid `var(--p-content-border-color)`

**File changes:**

- `src/components/skills/AbilitiesSection.vue` — CSS changes only

**CSS changes (add to existing scoped `<style>`):**

```css
/* Color tokens at top of style block */
.section-block {
  border-top: 1px solid var(--p-content-border-color);
  border-left: 4px solid transparent; /* base, overridden per section */
  padding-top: 0.75rem;
  padding-left: 1rem;
  margin-left: -0.25rem; /* offset padding so border aligns with content edge */
  background: transparent;
  transition: background 0.15s ease;
}

.section-block--lore {
  border-left-color: var(--p-cyan-400);
  background: color-mix(in srgb, var(--p-cyan-400) 4%, transparent);
}
.section-block--requirements {
  border-left-color: var(--p-orange-400);
  background: color-mix(in srgb, var(--p-orange-400) 4%, transparent);
}
.section-block--mechanics {
  border-left-color: var(--p-purple-400);
  background: color-mix(in srgb, var(--p-purple-400) 4%, transparent);
}
.section-block--feedback {
  border-left-color: var(--p-green-400);
  background: color-mix(in srgb, var(--p-green-400) 4%, transparent);
}
.section-block--on-failure {
  border-left-color: var(--p-red-400);
  background: color-mix(in srgb, var(--p-red-400) 4%, transparent);
}

.section-label {
  color: var(--p-text-color); /* inherited from parent section accent */
}
.section-block--lore .section-label { color: var(--p-cyan-400); }
.section-block--requirements .section-label { color: var(--p-orange-400); }
.section-block--mechanics .section-label { color: var(--p-purple-400); }
.section-block--feedback .section-label { color: var(--p-green-400); }
.section-block--on-failure .section-label { color: var(--p-red-400); }
```

**Template changes:**

Add a class to each `section-block` div based on its section type:

```diff
-<div class="section-block">
+<div class="section-block section-block--lore">
   <label class="section-label">Lore Lines</label>

-<div class="section-block">
+<div class="section-block section-block--requirements">
   <label class="section-label">Requirements</label>

-<div class="section-block">
+<div class="section-block section-block--mechanics">
   <label class="section-label">Mechanics</label>

-<div class="section-block">
+<div class="section-block section-block--feedback">
   <label class="section-label">Feedback</label>

-<div class="section-block">
+<div class="section-block section-block--on-failure">
   <label class="section-label">On Failure</label>
```

---

#### Commit 2 — Differentiated Inner Cards with Left Accent Bars

**Goal:** Make each card type visually distinguishable at a glance by
giving each a distinct colored left border (3px) matching its parent
section's accent.

Card type color mapping:

| CSS Class | Color | CSS Variable |
|-----------|-------|-------------|
| `.item-card` | Orange | `var(--p-orange-400)` |
| `.mechanic-card` | Purple | `var(--p-purple-400)` |
| `.particle-card` | Green | `var(--p-green-400)` |
| `.sound-card` | Teal | `var(--p-teal-400)` or `var(--p-cyan-400)` |
| `.failure-card` | Red | `var(--p-red-400)` |
| `.param-entry` | Indigo (dotted) | `var(--p-indigo-300)` |

**Behavior:**
- Each card type gets a 3px `border-left` in its accent color
- `.param-entry` inside `.mechanic-card` gets additional left padding
  (1rem) and a thinner dotted left border (2px) to signal deeper nesting
- The existing shared border/padding/background rules for these cards
  remain; only the left border accent is added

**CSS changes:**

```css
.item-card {
  border-left: 3px solid var(--p-orange-400);
}
.mechanic-card {
  border-left: 3px solid var(--p-purple-400);
}
.particle-card {
  border-left: 3px solid var(--p-green-400);
}
.sound-card {
  border-left: 3px solid var(--p-cyan-400);
}
.failure-card {
  border-left: 3px solid var(--p-red-400);
}

.param-entry {
  border-left: 2px dotted var(--p-indigo-300);
  margin-left: 0.5rem;
}
```

**Note:** The shared `.item-card, .mechanic-card, .particle-card, .sound-card`
group rule should remain for shared properties (border, border-radius,
padding, margin-bottom, background) — only override `border-left`
individually.

Move the existing `.param-entry` rule to add the dotted left border and
indentation:

```diff
.param-entry {
+  border-left: 2px dotted var(--p-indigo-300);
+  margin-left: 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.5rem;
  background: var(--p-content-background);
  margin-bottom: 0.5rem;
}
```

**Note:** `.param-entry` previously used `border` shorthand. We must be
careful about specificity — either combine the border-left with the
generic border property or use longhand. Correct approach:

```css
.param-entry {
  border: 1px solid var(--p-content-border-color);
  border-left: 2px dotted var(--p-indigo-300);
  border-radius: 4px;
  padding: 0.5rem;
  margin-left: 0.5rem;
  margin-bottom: 0.5rem;
  background: var(--p-content-background);
}
```

---

#### Commit 3 — Depth Layering via Background Tint

**Goal:** Create a visual "drill-down" effect by increasing background
tint at each nesting level. The eye naturally follows the gradient to
understand containment.

**Tint levels:**

| Level | Selector | Effect |
|-------|----------|--------|
| 0 | `.ability-body` | Base `var(--p-content-background)` |
| 1 | `.section-block` | +3% tint of accent color |
| 2 | `.item-card`, `.mechanic-card`, `.particle-card`, `.sound-card`, `.failure-card` | +7% tint of accent color |
| 3 | `.param-entry` (nested inside `.mechanic-card`) | +10% tint or `var(--p-form-field-background)` |

**CSS changes:**

Update the section-block tint from 4% to 3% (from commit 1), then add
nested card tints. The tint compounds using `color-mix` with the card's
parent section accent color.

Approach: use CSS custom properties for the accent color, set on the
section-block, and the inner cards inherit it via `var()`.

```css
.section-block--lore      { --section-accent: var(--p-cyan-400); }
.section-block--requirements { --section-accent: var(--p-orange-400); }
.section-block--mechanics   { --section-accent: var(--p-purple-400); }
.section-block--feedback    { --section-accent: var(--p-green-400); }
.section-block--on-failure  { --section-accent: var(--p-red-400); }

/* Level 1: section-block background */
.section-block {
  background: color-mix(in srgb, var(--section-accent) 3%, var(--p-content-background));
}

/* Level 2: inner cards */
.item-card,
.mechanic-card,
.particle-card,
.sound-card,
.failure-card {
  background: color-mix(in srgb, var(--section-accent) 7%, var(--p-content-background));
}

/* Level 3: param-entry inside mechanic (deeper nesting) */
.param-entry {
  background: color-mix(in srgb, var(--section-accent) 10%, var(--p-content-background));
}
```

**Important:** The existing `.item-card` etc. rule sets `background:
var(--p-content-background)`. This must be changed to use the tint
instead. The rule will need to be overridden.

Current:
```css
.item-card,
.mechanic-card,
.particle-card,
.sound-card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
  background: var(--p-content-background);
}
```

Change to:
```css
.item-card,
.mechanic-card,
.particle-card,
.sound-card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
}
```

And add the background separately with the tint:
```css
.item-card,
.mechanic-card,
.particle-card,
.sound-card,
.failure-card {
  background: color-mix(in srgb, var(--section-accent) 7%, var(--p-content-background));
}
```

---

#### Commit 4 — Per-Section Collapse Toggle

**Goal:** Allow users to independently collapse/expand each section
within an ability card (Lore Lines, Requirements, Mechanics, Feedback,
On Failure), reducing visual noise when editing specific parts of a
complex ability.

**Behavior:**
- Each section label becomes clickable (cursor: pointer)
- A small expand/collapse arrow (▶/▼) appears before the section label text
- Clicking the section header toggles that section's visibility within
  the ability card
- Section collapse state is stored per-section per-ability in a reactive
  Map<string, boolean> keyed by `"${abilityIdx}-${sectionKey}"`
- All sections default to expanded (true) for new abilities
- Collapsed sections show just the section header (label + arrow + count badge)
- The count badge shows number of items inside (e.g., "Mechanics (3)",
  "Feedback", "Particles (2)")

**State management additions (script setup):**

```typescript
// New reactive state
const sectionExpanded = ref<Record<string, boolean>>({})

function toggleSection(abilityIdx: number, sectionKey: string) {
  const key = `${abilityIdx}-${sectionKey}`
  sectionExpanded.value[key] = !sectionExpanded.value[key]
}

function isSectionExpanded(abilityIdx: number, sectionKey: string): boolean {
  const key = `${abilityIdx}-${sectionKey}`
  return sectionExpanded.value[key] !== false // default true
}

// Helper section counts
function sectionCount(ability: Ability, sectionKey: string): number | null {
  switch (sectionKey) {
    case 'lore': return ability.lore.length
    case 'requirements': return null // always shows simple fields
    case 'mechanics': return ability.mechanics.length
    case 'feedback': return null // always shows toggles
    case 'particles': return ability.feedback.particles.length
    case 'sounds': return ability.feedback.sounds.length
    case 'on-failure': {
      const of = ability.onFailure
      return of ? Object.keys(of.reasons).length : 0
    }
    default: return null
  }
}
```

**Template changes (pattern for each section):**

```html
<!-- Before (Requirements section, for example) -->
<div class="section-block section-block--requirements">
  <label class="section-label">Requirements</label>
  ...content...
</div>

<!-- After -->
<div class="section-block section-block--requirements">
  <div
    class="section-header"
    @click="toggleSection(idx, 'requirements')"
  >
    <span class="section-toggle">{{ isSectionExpanded(idx, 'requirements') ? '▼' : '▶' }}</span>
    <label class="section-label">Requirements</label>
  </div>
  <template v-if="isSectionExpanded(idx, 'requirements')">
    ...content...
  </template>
</div>
```

Apply the same pattern to all 5 section-blocks and to the Particles and
Sounds sub-sections (both nested under Feedback).

For sub-sections (Particles, Sounds) that already have `.sub-section` and
`.sub-label`, also make them collapsible with the same pattern, using a
smaller toggle arrow.

**CSS additions:**

```css
.section-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  user-select: none;
  margin-bottom: 0.5rem;
}
.section-header:hover .section-label {
  opacity: 0.8;
}
.section-toggle {
  font-size: 0.65rem;
  color: var(--p-form-field-placeholder-color);
  flex-shrink: 0;
  width: 0.75rem;
  text-align: center;
}
.section-count {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--p-form-field-placeholder-color);
  margin-left: 0.25rem;
}
```

**Count badge display in section headers:**

When displaying a section with items (Lore, Mechanics, Particles, Sounds,
On Failure), show the count:

```html
<div
  class="section-header"
  @click="toggleSection(idx, 'mechanics')"
>
  <span class="section-toggle">{{ isSectionExpanded(idx, 'mechanics') ? '▼' : '▶' }}</span>
  <label class="section-label">Mechanics</label>
  <span class="section-count">({{ ability.mechanics.length }})</span>
</div>
```

**Important state consideration:**
- The `sectionExpanded` map should NOT be keyed by `ability.id` (which can
  change as the user types). Use `abilityIdx` only.
- When abilities are reordered (drag), the expanded states for each index
  should be preserved since we key by index. This is acceptable behavior.

---

#### Commit 5 — Section SVG Icons

**Goal:** Add inline SVG icons before each section label to provide a
rapid-scanning visual cue. These use `currentColor` and match the
monochrome aesthetic of the existing interface.

**This commit is last and can be reverted with `git revert HEAD` without
affecting the other four improvements.**

Icon design:

| Section | SVG Shape | Visual metaphor |
|---------|-----------|-----------------|
| Lore Lines | Document with text lines | `rect` + 3 horizontal `line`s |
| Requirements | Shield | `path` chevron shield shape |
| Mechanics | Gear | `circle` + interior `path` teeth |
| Feedback | Bell | `path` bell shape |
| On Failure | X-circle | `circle` + `line` cross |

Each icon is a 14×14 inline `<svg>` with `fill="none"` and
`stroke="currentColor"`, placed as the first child inside the section header
(before the toggle arrow, or after it — decide based on visual balance).

**Template addition pattern:**

```html
<div class="section-header" @click="toggleSection(idx, 'mechanics')">
  <span class="section-toggle">▼</span>
  <svg class="section-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.3">
    <!-- Gear icon: circle + teeth -->
    <circle cx="8" cy="8" r="3" />
    <path d="M8 1.5v2M8 12.5v2M1.5 8h2M12.5 8h2M3.05 3.05l1.41 1.41M11.54 11.54l1.41 1.41M3.05 12.95l1.41-1.41M11.54 4.46l1.41-1.41" />
  </svg>
  <label class="section-label">Mechanics</label>
  <span class="section-count">({{ ability.mechanics.length }})</span>
</div>
```

**Icon SVGs:**

Lore (document):
```svg
<rect x="2" y="2" width="12" height="12" rx="1" stroke="currentColor" fill="none" stroke-width="1.2"/>
<line x1="4.5" y1="6" x2="11.5" y2="6" stroke="currentColor" stroke-width="1.2"/>
<line x1="4.5" y1="8.5" x2="11.5" y2="8.5" stroke="currentColor" stroke-width="1.2"/>
<line x1="4.5" y1="11" x2="9" y2="11" stroke="currentColor" stroke-width="1.2"/>
```

Requirements (shield):
```svg
<path d="M3 2.5h10L13 8a6 6 0 01-5 5.5A6 6 0 013 8L3 2.5z" stroke="currentColor" fill="none" stroke-width="1.2"/>
<line x1="5.5" y1="7" x2="7.5" y2="9" stroke="currentColor" stroke-width="1.2"/>
<line x1="7.5" y1="9" x2="10.5" y2="5.5" stroke="currentColor" stroke-width="1.2"/>
```

Mechanics (gear):
```svg
<circle cx="8" cy="8" r="3" stroke="currentColor" fill="none" stroke-width="1.2"/>
<path d="M8 2v2M8 12v2M2 8h2M12 8h2M3.76 3.76l1.41 1.41M10.83 10.83l1.41 1.41M3.76 12.24l1.41-1.41M10.83 5.17l1.41-1.41" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
```

Feedback (bell):
```svg
<path d="M4 8a4 4 0 018 0c0 2 1 3 1 3H3s1-1 1-3z" stroke="currentColor" fill="none" stroke-width="1.2"/>
<line x1="6.5" y1="12" x2="9.5" y2="12" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
<line x1="8" y1="2" x2="8" y2="3.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
```

On Failure (x-circle):
```svg
<circle cx="8" cy="8" r="6" stroke="currentColor" fill="none" stroke-width="1.2"/>
<line x1="5.5" y1="5.5" x2="10.5" y2="10.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
<line x1="10.5" y1="5.5" x2="5.5" y2="10.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
```

**CSS additions:**

```css
.section-icon {
  flex-shrink: 0;
  opacity: 0.7;
}
.section-header:hover .section-icon {
  opacity: 1;
}
```

---

## Implementation Order & Dependencies

Each commit builds on the previous one, but each is independently
revertible (no structural refactors between commits).

```
Commit 1 ──→ Commit 2 ──→ Commit 3 ──→ Commit 4 ──→ Commit 5
  gutters      card types     depth tint    collapse       icons
```

No commit depends on the template structure changes of a later commit,
so reverting any single commit is safe.

## Files Modified

| File | Commits | Changes |
|------|---------|---------|
| `web/frontend/src/components/skills/AbilitiesSection.vue` | 1–5 | CSS additions, template class additions, section-header wrappers, collapse state management, SVG icons |

This is the only file that needs modification — all changes are
self-contained within `AbilitiesSection.vue`.

## Verification

After each commit:
1. Run `npm run build` (or `cd web/frontend && npm run build`) to verify
   the Vue project compiles without errors
2. Visually inspect one simple ability (geologist) and one complex ability
   (vein miner) in the skill editor
3. Verify the feature works:
   - **Commit 1:** Colored left borders visible on each section
   - **Commit 2:** Card types distinguishable by left accent color
   - **Commit 3:** Background tints deepen at each nesting level
   - **Commit 4:** Clicking section headers collapses/expands content;
     state persists across collapse/expand cycles
   - **Commit 5:** Icons render correctly next to each section label

After all commits: `git log --oneline` should show 5 clean commits.
