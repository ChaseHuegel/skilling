# ISSUE-017: Abilities Page Search

**Scope:** Add text search to the abilities overview page, matching the same pattern used on the dashboard and tags pages.

---

## Requirements

- Text input at the top of the abilities page, below the header subtitle
- Searches across the following fields of each ability entry:
  - **Ability ID** (`ability.id`)
  - **Ability display name** (`ability.displayName`)
  - **Associated skill ID** (`skillId`)
  - **Associated skill display name** (`skillDisplayName`)
  - **Unlock level** (`ability.unlockLevel` — match as string)
  - **Requirement states** (`ability.requirements.states[]` — list of strings like `"is_sneaking"`)
  - **Feedback message** (`ability.feedback.message`)
  - **Mechanic types** (`ability.mechanics[].type` — e.g. `"core:chain_break"`)
- Case-insensitive substring matching
- Real-time filtering as the user types (no button press)
- When search is active and no results match, show "No abilities match your search"
- Search works with the existing card grid layout — just hide non-matching cards

## UI

```
┌──────────────────────────────────────────────┐
│ Abilities                           [3 abils] │
│ All abilities across all skills               │
│                                                │
│ 🔍 Search abilities...                         │
│                                                │
│ ┌─────────────────┐ ┌─────────────────┐       │
│ │ Vein Miner      │ │ Geologist       │       │
│ │ Skill: mining   │ │ Skill: mining   │       │
│ │ Unlock: Level 15│ │ Unlock: Level 1 │       │
│ │ Active • 5s CD  │ │ Passive         │       │
│ └─────────────────┘ └─────────────────┘       │
└──────────────────────────────────────────────┘
```

## Files

- `src/views/AbilitiesPage.vue` — add search input + filtered ability list
- `src/components/skills/AbilityCard.vue` — no changes needed, filtering is in the parent

## Implementation

### 1. Extend the ability data interface

The current `AbilityEntry` only stores a subset of ability fields. For search, the full `AbilityDTO` structure from the API response must be stored so all searchable fields are available on each entry:

```typescript
interface AbilityEntry {
    skillId: string;
    skillDisplayName: string;
    ability: {
        id: string;
        displayName?: string;
        unlockLevel: number;
        requirements?: {
            cooldown?: number;
            state?: string[];
            items?: any[];
        };
        mechanics?: {
            type: string;
            filters?: any[];
            parameters?: any;
        }[];
        feedback?: {
            actionBar?: boolean;
            chat?: boolean;
            message?: string;
            particles?: any[];
            sounds?: any[];
        };
    };
}
```

### 2. Search logic

```typescript
const searchQuery = ref('');

const filteredAbilities = computed(() => {
    if (!searchQuery.value.trim()) return abilities.value;
    const q = searchQuery.value.toLowerCase();
    return abilities.value.filter((a) => {
        const id = (a.ability.id || '').toLowerCase();
        const displayName = (a.ability.displayName || '').toLowerCase();
        const skillId = (a.skillId || '').toLowerCase();
        const skillDisplay = (a.skillDisplayName || '').toLowerCase();
        const unlockLevel = String(a.ability.unlockLevel);
        const states = (a.ability.requirements?.state || []).join(' ').toLowerCase();
        const feedbackMsg = (a.ability.feedback?.message || '').toLowerCase();
        const mechanics = (a.ability.mechanics || []).map((m: any) => m.type).join(' ').toLowerCase();
        const fields = [id, displayName, skillId, skillDisplay, unlockLevel, states, feedbackMsg, mechanics];
        return fields.some(f => f.includes(q));
    });
});
```

### 3. Template changes

- Insert search input after `.page-subtitle` and before the loading/grid block
- Bind `v-for` to `filteredAbilities` instead of `abilities`
- Add "No abilities match your search" empty state when search is active and results are empty

### 4. CSS

- Reuse the same search bar styling pattern from `DashboardPage.vue` (`.search-bar`, `.search-icon`, `.search-input`)

---

## No Backend Changes Required

The abilities page already fetches full skill detail objects from `GET /api/skills/{id}`, which includes all the fields needed for search: `ability.id`, `ability.displayName`, `ability.requirements.states`, `ability.feedback.message`, `ability.mechanics[].type`. No DTO or handler changes are needed.
