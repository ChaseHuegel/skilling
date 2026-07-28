# ISSUE-015: Abilities Overview Page + Ability Editor Fix

**Scope:** New abilities overview page and fixing the ability editor regression.

---

## 1. Abilities Overview Page

### Requirements

A new page at `/#/abilities` that shows all abilities across all skills
in a grid layout, similar to the skill dashboard.

### Route

```
/#/abilities  →  AbilitiesPage.vue
```

### UI

```
┌─────────────────────────────────────────────┐
│ Abilities                                   │
│                                              │
│ ┌─────────────────┐ ┌─────────────────┐      │
│ │ Vein Miner      │ │ Geologist       │      │
│ │ Skill: mining   │ │ Skill: mining   │      │
│ │ Unlock: Level 15│ │ Unlock: Level 1 │      │
│ │ Active • 5s CD  │ │ Passive         │      │
│ └─────────────────┘ └─────────────────┘      │
└─────────────────────────────────────────────┘
```

### Behavior

- Fetches all skills from `GET /api/skills`
- Iterates over each skill's abilities
- Each card shows: ability name, owning skill name, unlock level, active/passive tag
- Clicking an ability navigates to `/#/skills/{skillId}` and scrolls to the ability editor section
- Add link to the abilities page in AppTopbar navigation

### Files

- `src/views/AbilitiesPage.vue` — new page
- `src/components/skills/AbilityCard.vue` — new component
- `src/router.ts` — add `/abilities` route
- `src/components/layout/AppTopbar.vue` — add "Abilities" nav link
- `src/views/SkillEditorPage.vue` — add scroll-to-ability support via route hash

---

## 2. Ability Editor Regression Fix

### Observation

Clicking to expand an ability card in the skill editor causes it to disappear
entirely. This is a regression from a previous working state.

### Root Cause Investigation

Most likely causes:
1. The expand/collapse toggle mutates the abilities array incorrectly
   (removes the ability instead of toggling a local `expanded` state)
2. Vue reactivity issue with nested array mutation
3. The `v-if`/`v-show` condition is bound to the wrong variable

### Fix

Ensure each ability in the `AbilitiesSection.vue` component has a local
`expanded` ref that defaults to `false` and toggles on click. The ability
object passed via `v-for` should NOT be mutated — use a separate
`expandedStates` map keyed by ability ID.

### Files

- `src/components/skills/AbilitiesSection.vue`
