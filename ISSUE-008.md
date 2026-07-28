# ISSUE-008: Dashboard Visual Redesign

**Epic / Milestone** — Estimated: 2-3 days of development

---

## Overview

The web GUI dashboard currently renders skill data but lacks visual polish:
icons are raw text strings, cards have minimal interactivity, and
loading/error/empty states are basic text placeholders. This issue
overhauls the dashboard with Minecraft item textures, improved card
design, skeleton loading, and clear empty/error states.

---

## Design Tenets

1. **Minecraft-native feel.** Use actual item textures from the Minecraft
   assets CDN so admins can preview how their icon choice looks in-game.
2. **Progressive enhancement.** The UI degrades gracefully: if the CDN is
   unreachable, styled letter fallbacks appear instead of broken images.
3. **No backend changes.** All changes are purely frontend (Vue components,
   CSS, Vite `.env` config). Zero modifications to Java code.
4. **E2E test stability.** Every phase passes the full Playwright suite
   before moving to the next.

---

## Phase A: Minecraft Item Textures & Documentation

### Files to Create

**`web/frontend/.env`**
```
VITE_MINECRAFT_ASSETS_VERSION=1.21.4
```

**`web/frontend/src/components/common/MinecraftIcon.vue`**

A reusable component that renders Minecraft item textures from the
InventivetalentDev/minecraft-assets CDN.

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `material` | `string` | required | e.g., `"minecraft:iron_pickaxe"` |
| `color` | `string` | `'#fff'` | Skill color for accent glow |
| `size` | `number` | `40` | Display dimension in px |

**Implementation details:**
- Strips `minecraft:` namespace, constructs GitHub raw CDN URL:
  `https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/assets/minecraft/textures/item/{itemName}.png`
- Version sourced from `import.meta.env.VITE_MINECRAFT_ASSETS_VERSION` with fallback `'1.21.4'`
- `image-rendering: pixelated` for crisp pixel art at scaled sizes
- Inventory-slot-style background: dark rounded container with subtle inset shadow
- Color accent: thin glow ring or border using `skill.color` at 30% opacity
- **Loading state:** CSS shimmer/pulse on the slot background
- **Error state:** Styled circle with first letter of item name, colored with `skill.color`
- **Loaded state:** Fade-in transition (`opacity 0→1`) to prevent texture pop-in

### Documentation Updates

| File | Change |
|------|--------|
| `web/AGENTS.md` | Add "Minecraft Asset Textures" section — `.env` variable, CDN source, version change instructions |
| `AGENTS.md` (root) | Add cross-reference under web section pointing to web/AGENTS.md |
| `.gitignore` | Add `web/frontend/.env` |

---

## Phase B: Skill Card Redesign

### File to Modify

**`web/frontend/src/components/skills/SkillCard.vue`**

### Changes

| Area | Before | After |
|------|--------|-------|
| **Icon** | Raw material text (e.g., `iron_pickaxe`) | `<MinecraftIcon :material="skill.icon" :color="skill.color" :size="48" />` |
| **Color accent** | 4px left border | 3px top border + icon slot glow |
| **Layout** | Horizontal flex row | Vertical flex column with header/body sections |
| **Ability count** | In meta text | Small colored chip badge in card header |
| **Level info** | `"Max Level: X"` | `"Level 1 – {maxLevel}"` |
| **Hover** | Subtle shadow | `translateY(-3px)` + `box-shadow: 0 8px 24px` |
| **Transition** | `box-shadow 0.15s` | `all 0.2s ease` |

### Card Structure (after)
```
┌─────────────────────┐
│ [MC Icon 48px]  [3] │ ← ability badge top-right
├─────────────────────┤
│ Mining              │ ← skill name
│ Level 1 – 100       │ ← level range
└─────────────────────┘
         ↑ 3px color accent border on top
```

---

## Phase C: Dashboard Header Polish

### File to Modify

**`web/frontend/src/views/DashboardPage.vue`**

### Changes

| Element | Before | After |
|---------|--------|-------|
| **Title** | Bare `<h1>Skills</h1>` | Title + skill count badge in flex row |
| **Subtitle** | None | `<p>Manage your skill definitions and abilities</p>` |
| **Button** | Text `+ New Skill` | Inline SVG icon + text, green accent, hover scale |
| **Skill count** | Not shown | Small chip: `"3 skills"` next to heading |

---

## Phase D: Loading Skeleton + Empty/Error States

### File to Modify

**`web/frontend/src/views/DashboardPage.vue`**

### Loading Skeleton

Replace `"Loading skills..."` text with 3 skeleton cards matching real
card dimensions to prevent layout shift.

Each skeleton card contains:
- Circular icon placeholder (48px)
- Text line placeholders (70% and 40% width)
- CSS shimmer animation (gradient sweep `1.5s` infinite)

Shimmer keyframes:
```css
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
```

### Empty State

Replace `"No skills loaded. Create one to get started."` with:
```
┌─────────────────────┐
│        📦           │
│  No skills yet      │
│  Create your first   │
│  skill definition.   │
│  [+ Create Skill]    │
└─────────────────────┘
```

### Error State

Replace bare error text with:
```
┌─────────────────────┐
│        ⚠️           │
│  {error message}    │
│  [Retry]             │
└─────────────────────┘
```

Refactor `onMounted` to use a named `fetchSkills()` function so the
retry button can re-invoke it.

---

## Phase E: Transitions & Responsive Refinement

### File to Modify

**`web/frontend/src/views/DashboardPage.vue`**

### Card Entrance Animation

Staggered fade-in + translateY for skill cards:

```css
.skill-card {
  animation: cardEnter 0.35s ease both;
}
.skill-card:nth-child(1) { animation-delay: 0ms; }
.skill-card:nth-child(2) { animation-delay: 50ms; }
.skill-card:nth-child(3) { animation-delay: 100ms; }
/* continue up to reasonable N */

@keyframes cardEnter {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
```

### Responsive Breakpoints

| Viewport | Columns | Behavior |
|----------|---------|----------|
| ≥ 960px | 3 | Standard grid |
| 640–960px | 2 | Tablet |
| < 640px | 1 | Mobile — header stacks vertically |

---

## Commit Plan

| # | Phase | Commit Message |
|---|-------|----------------|
| 1 | A+H | `feat(web): MinecraftIcon component with CDN texture support, .env config, docs` |
| 2 | B | `feat(web): redesign SkillCard with Minecraft icons, hover lift, ability badge` |
| 3 | C | `feat(web): polish dashboard header — subtitle, skill count badge, SVG icon` |
| 4 | D+E | `feat(web): loading skeleton shimmer, empty/error state redesign, card entrance animation, responsive grid` |

Each commit must:
1. Build the frontend (`npm run build`)
2. Build the Gradle project (`./gradlew build`)
3. Run the full E2E suite (`npx playwright test`)
4. All tests pass before the next phase begins

---

## Rollback

Each phase is a separate commit. To roll back any phase, revert that
commit with `git revert <hash>`. The E2E suite will verify the
rolled-back state is still functional.
