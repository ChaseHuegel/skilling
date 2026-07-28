# ISSUE-020: Skill Card Actions — Duplicate & Delete

**Scope:** Add duplicate and delete buttons to skill cards on the dashboard. Duplicate skill generates a new skill with an incremented ID. Delete removes the skill file with confirmation.

---

## Requirements

### Duplicate Button
- Icon button on each skill card (top-right, next to badges, or bottom-left)
- On click, fetches the full skill detail via `GET /api/skills/{id}`
- Creates a new skill via `POST /api/skills` with:
  - ID: original ID + `_1` (or `_2`, `_3` etc. — increment until no conflict)
  - Display name: original + ` (copy)`
  - All other fields copied verbatim
- Navigates to the new skill editor after creation

### Delete Button
- Icon button on each skill card (next to duplicate button)
- Shows a confirmation dialog before deleting
- On confirm, calls `DELETE /api/skills/{id}`
- Removes the card from the grid, fetches updated skill list
- Confirmation dialog: "Delete skill?" / "This will permanently remove {skill name} and all its data." / [Keep] [Delete]

### Skill ID Display
- Show the skill's `id` in small, faded text at the bottom of each card
- Format: dark grey, smaller font, monospace
- Helps differentiate duplicates

## UI Mock

```
┌────────────────────┐
│ [icon]        [dup][del] │
│ Mining               │
│ 2 XP sources         │
│ 2 abilities          │
│ Level 1 – 100        │
│ mining               │  ← small faded ID
└────────────────────┘
```

## Files

- `src/components/skills/SkillCard.vue` — add buttons, ID display, emit events
- `src/views/DashboardPage.vue` — handle duplicate/delete events, refresh list
- `src/views/SkillEditorPage.vue` — no changes (already handles create)

## Implementation

### SkillCard.vue Changes

Add emits:
```typescript
const emit = defineEmits<{
  duplicate: [skillId: string]
  delete: [skillId: string]
}>()
```

Template additions:
- Duplicate icon button: `on-click="emit('duplicate', skill.id)"`
- Delete icon button: `on-click="emit('delete', skill.id)"` (with `@click.stop` to prevent card navigation)
- Faded ID text at bottom of `.card-body`

Display the skill `id` at the bottom of the card:
```html
<div class="skill-id">{{ skill.id }}</div>
```

### DashboardPage.vue Changes

```typescript
async function duplicateSkill(id: string) {
  const detail = await api.skills.get(id);
  let copyId = id + '_1';
  let attempts = 0;
  const existingIds = new Set(skills.value.map((s: any) => s.id));
  while (existingIds.has(copyId) && attempts < 100) {
    const num = parseInt(copyId.replace(/.*_(\d+)$/, '$1')) + 1;
    copyId = id + '_' + num;
    attempts++;
  }
  detail.id = copyId;
  detail.displayName = (detail.displayName || id) + ' (copy)';
  await api.skills.create(detail);
  router.push(`/skills/${copyId}`);
}

function confirmDeleteSkill(id: string) {
  deleteTarget.value = id;
  showDeleteDialog.value = true;
}

async function deleteSkill() {
  const id = deleteTarget.value;
  if (!id) return;
  showDeleteDialog.value = false;
  await api.skills.delete(id);
  await fetchSkills();
}
```

### Confirmation Dialog

Add dialog to DashboardPage.vue:
```html
<div v-if="showDeleteDialog" class="modal-overlay" @click.self="showDeleteDialog = false">
  <div class="modal">
    <h3>Delete skill?</h3>
    <p>This will permanently remove <strong>{{ deleteTargetName }}</strong> and all its data.</p>
    <div class="modal-actions">
      <button class="btn btn-secondary" @click="showDeleteDialog = false">Keep</button>
      <button class="btn btn-danger" @click="deleteSkill">Delete</button>
    </div>
  </div>
</div>
```
