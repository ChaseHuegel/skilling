# ISSUE-014: Drag Reordering

**Scope:** Add drag-and-drop reordering to lists while maintaining existing layouts.

---

## Design Constraints

- Use native HTML5 Drag and Drop API — no additional dependencies
- Visual drag handle (≡) to indicate draggable items
- Smooth visual feedback during drag (opacity change, placeholder)
- Works on all pages where lists exist
- Skill cards maintain their grid layout but can be reordered within it
- Reordered state is stored in a Pinia store or local state until "Save" is clicked

---

## 1. XP Source Cards (Skill Editor)

**File:** `src/components/skills/XpSourcesSection.vue`

**Behavior:**
- Each XP source card gets a drag handle (≡) in the top-left
- Drag and drop to reorder within the list
- Emits `update:modelValue` with the reordered array

---

## 2. Ability Cards (Skill Editor)

**File:** `src/components/skills/AbilitiesSection.vue`

**Behavior:**
- Each ability card gets a drag handle (≡) in its header
- Drag and drop to reorder within the list
- Emits `update:modelValue` with the reordered array

---

## 3. Tag Cards (Tags Page)

**File:** `src/components/tags/TagListEditor.vue`

**Behavior:**
- Each tag card gets a drag handle (≡)
- Tags can be reordered within the list
- The tag's CONTENT (materials list) is NOT draggable — only the tag itself

---

## 4. Skill Cards (Dashboard)

**File:** `src/views/DashboardPage.vue`

**Behavior:**
- Skill cards remain in a CSS Grid layout
- Drag handle is visible on hover over each card
- Cards swap positions when dragged over another card
- The grid reflows naturally using CSS `order` property or array reordering
- Reordered state is in local state only (not saved to backend)

---

## Implementation Approach

Create a composable `useDragReorder.ts`:

```typescript
export function useDragReorder<T>(items: Ref<T[]>) {
    const dragIndex = ref<number | null>(null);
    
    function onDragStart(index: number) {
        dragIndex.value = index;
    }
    
    function onDragOver(e: DragEvent, index: number) {
        e.preventDefault();
        if (dragIndex.value === null || dragIndex.value === index) return;
        const newItems = [...items.value];
        const [removed] = newItems.splice(dragIndex.value, 1);
        newItems.splice(index, 0, removed);
        items.value = newItems;
        dragIndex.value = index;
    }
    
    function onDragEnd() {
        dragIndex.value = null;
    }
    
    return { dragIndex, onDragStart, onDragOver, onDragEnd };
}
```

Then use it in each component that needs reordering.
