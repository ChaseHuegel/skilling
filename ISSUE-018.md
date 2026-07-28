# ISSUE-018: Material Icon Picker

**Scope:** Replace the free-text icon input in the skill editor with a searchable dropdown populated with all Minecraft material names and 16×16 inline texture thumbnails.

---

## Requirements

- Dropdown replaces the current plain `<input type="text">` for the `icon` field in `DisplaySection.vue`
- Dropdown is populated with all Minecraft material/item names (namespaced, e.g. `minecraft:iron_pickaxe`)
- Each option shows a 16×16 inline texture thumbnail fetched from the same CDN used by `MinecraftIcon.vue`
- Dropdown includes a text search/filter field at the top to narrow down materials
- Selected value is displayed in the dropdown trigger with its thumbnail and name
- Uses the existing `MinecraftIcon` component for rendering thumbnails
- Handles non-item materials gracefully (blocks, etc. — all have textures on the CDN)

## UI

```
┌──────────────────────────────────────────┐
│ Icon                                      │
│ ┌──────────────────────────────────────┐ │
│ │ [pickaxe icon] minecraft:iron_pickaxe │▼│ │
│ └──────────────────────────────────────┘ │
│ ┌──────────────────────────────────────┐ │
│ │ 🔍 Search materials...               │ │
│ │ ──────────────────────────────────── │ │
│ │ [icon] minecraft:acacia_boat         │ │
│ │ [icon] minecraft:acacia_button       │ │
│ │ [icon] minecraft:acacia_door         │ │
│ │ ...                                  │ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

## Files

- `src/components/skills/DisplaySection.vue` — replace icon text input with `MaterialPicker` component
- `src/components/common/MaterialPicker.vue` — new dropdown component
- `src/components/common/MinecraftIcon.vue` — already exists, reused for thumbnails

## Material List Sourcing

Maintain a static JSON array of material names in the frontend source. Generating it at runtime via the API would be expensive. Options:

### Option A: Bundled JSON (Recommended)
- Use a script to extract item names from `https://raw.githubusercontent.com/InventivaletalentDev/minecraft-assets/{VERSION}/assets/minecraft/textures/item/`
- Generate a `materials.json` that contains all material names with `minecraft:` prefix
- Bundle into the frontend build (import as static asset)
- Fallback: hardcode a comprehensive list of common materials

### Option B: API Endpoint
- Add a `GET /api/materials` endpoint that returns all registered `Material` names from the Bukkit API
- Frontend fetches once and caches

**Recommendation:** Option A for simplicity and offline support. The list of Minecraft items rarely changes between patch versions, and the `.env` already specifies the asset version.

## Implementation Approach

1. Create `src/assets/materials.json` with a curated list of ~200 common Minecraft items/materials with `minecraft:` namespace
2. Create `MaterialPicker.vue`:
   - Props: `modelValue` (string), `materials` (string[])
   - Emits: `update:modelValue`
   - Internal state: `searchQuery`, `isOpen`
   - Filter materials by search query (case-insensitive)
   - Show filtered list in a scrollable dropdown (max-height with overflow)
   - Each item renders a `MinecraftIcon` (16×16) + material name
   - Click selects and closes dropdown
   - Click outside closes dropdown
3. Update `DisplaySection.vue` to replace the icon input with `<MaterialPicker>`
