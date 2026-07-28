# ISSUE-012: Search — Skill Dashboard + Tags Dashboard

**Scope:** Add text search to both the skill dashboard and tags dashboard pages.

---

## Skill Dashboard Search

### Requirements

- Text input at the top of the dashboard, below the header subtitle
- Searches across: skill `id`, `displayName`, XP source `trigger` values, ability `id`s, ability `displayName`s
- Case-insensitive substring matching
- Real-time filtering as the user types (no button press required)
- When search is active and no results match, show a "No skills match your search" message
- Search should work with the existing card grid layout — just hide non-matching cards

### UI

```
┌─────────────────────────────────────────────┐
│ Skills                    [3 skills] [+ New] │
│ Manage your skill definitions and abilities  │
│                                              │
│ 🔍 Search skills...                          │
│                                              │
│ ┌────────┐ ┌────────┐ ┌────────┐            │
│ │ Mining │ │Woodcut.│ │Farming │            │
│ └────────┘ └────────┘ └────────┘            │
└─────────────────────────────────────────────┘
```

### Files

- `src/views/DashboardPage.vue` — add search input + filtered skill list
- `src/components/skills/SkillCard.vue` — no changes needed, filtering is in the parent

### Implementation

```typescript
const searchQuery = ref('');

const filteredSkills = computed(() => {
    if (!searchQuery.value.trim()) return skills.value;
    const q = searchQuery.value.toLowerCase();
    return skills.value.filter((s) => {
        const id = (s.id || '').toLowerCase();
        const display = (s.displayName || '').toLowerCase();
        const triggers = (s.xpSourceTriggers || []).join(' ').toLowerCase();
        const abilityIds = (s.abilityIds || []).join(' ').toLowerCase();
        const abilityNames = (s.abilityNames || []).join(' ').toLowerCase();
        return [id, display, triggers, abilityIds, abilityNames].some(f => f.includes(q));
    });
});
```

Need to extend `SkillSummaryDTO` (backend) and the API response to include `xpSourceTriggers`, `abilityIds`, and `abilityNames` arrays for the frontend to search against.

---

## Tags Dashboard Search

### Requirements

- Text input at the top of the tags page
- Searches across: tag name (the `#c:` key), and tag contents (material names)
- Case-insensitive substring matching
- Real-time filtering
- When no results match, show "No tags match your search"

### Files

- `src/views/TagsPage.vue` — add search input + filtered tag list

### Implementation

Same pattern as skill search — a computed `filteredTags` that filters the reactive `tags` object.
