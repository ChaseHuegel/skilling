# ISSUE-063: Web GUI Editor for gui.yml Layout

## Description

The in-game skills chest GUI (`/skills`) currently has a hardcoded layout that
does not scale for large skill sets. A `gui.yml` config file is proposed to let
server admins define named pages (tabs) and arrange skill icons onto a
54-slot chest grid per page. This issue covers the **Web GUI editor** for that
config file — a drag-and-drop visual editor that mirrors the in-game chest
inventory experience.

Admins should be able to:
- Navigate to a new "GUI Layout" page from the topbar
- See a rendered 54-slot (6×9) chest grid that represents the in-game menu
- Add/remove pages (tabs), rename page labels
- Drag skill icons from a palette/search panel onto grid slots
- Drag icons off the grid to remove them
- Drag icons between slots to swap positions
- Hover over any placed skill to see a tooltip formatted exactly like the
  plugin's lore snippets (`&` color codes → rendered HTML)

## Scope

- New route `/layout` with `GuiLayoutPage.vue` view
- New Pinia store `gui-layout.ts` for managing the layout state
- New API endpoints for reading/writing `gui.yml`
- New components: `ChestGrid.vue`, `ChestSlot.vue`, `PageTabs.vue`,
  `SkillPalette.vue`, `SkillTooltip.vue`
- Reuse existing `MinecraftIcon.vue`, `minecraftColors.ts` utilities
- Staging workflow integration (edits go to `.web_staging/` before reload)
- E2E tests for the new page in Playwright
- Backend handler and DTO for `gui.yml` read/write

Out of scope:
- The in-game rendering of `gui.yml` (separate plugin work)
- Resource pack / custom model data preview in the grid
- Multi-user concurrent editing

## Proposed Solution

### New API Endpoints

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| `GET` | `/api/gui-layout` | `GuiLayoutHandler.get` | Return the current `gui.yml` as a JSON object |
| `PUT` | `/api/gui-layout` | `GuiLayoutHandler.update` | Stage a new `gui.yml` from the editor payload |

The `gui.yml` schema on disk:

```yaml
# gui.yml — Skill Overview GUI Layout
title: "&8⚒ &6Skills &8⚒"
rows: 6                      # Chest rows (3-6)
pages:
  - label: "&eCombat"
    slots:
      0: "swords"
      1: "archery"
      9: "axes"
      # slot index 0-53, value is the skill id
  - label: "&aGathering"
    slots:
      0: "mining"
      1: "woodcutting"
      9: "farming"
```

The DTO passed over JSON mirrors this structure:

```typescript
interface GuiLayoutDTO {
  title: string;
  rows: number;
  pages: GuiPageDTO[];
}
interface GuiPageDTO {
  label: string;
  slots: Record<number, string>;  // slot-index → skill-id
}
```

### New Frontend Route

Add to `router.ts`:

```typescript
{
  path: '/layout',
  name: 'GuiLayout',
  component: () => import('./views/GuiLayoutPage.vue'),
  meta: { requiresAuth: true },
}
```

A new "Layout" nav link is added to `AppTopbar.vue` between "Abilities" and
"Tags".

### New Pinia Store: `web/frontend/src/stores/gui-layout.ts`

```typescript
export const useGuiLayoutStore = defineStore('guiLayout', () => {
  const layout = ref<GuiLayoutDTO | null>(null);
  const allSkills = ref<SkillSummary[]>([]);   // fetched via skills list API
  const loading = ref(false);
  const saving = ref(false);

  async function fetch() { /* GET /api/gui-layout */ }
  async function save()  { /* PUT /api/gui-layout */ }

  function addPage(label: string) { /* push new page */ }
  function removePage(index: number) { /* splice page */ }
  function renamePage(index: number, label: string) { /* update label */ }

  function setSlot(pageIndex: number, slot: number, skillId: string | null) { /* assign/remove */ }
  function swapSlots(pageIndex: number, a: number, b: number) { /* swap two slots */ }
  function clearSlot(pageIndex: number, slot: number) { /* remove assignment */ }

  return { layout, allSkills, loading, saving, fetch, save, addPage, removePage, renamePage, setSlot, swapSlots, clearSlot };
});
```

### API Client Addition: `web/frontend/src/api/client.ts`

```typescript
guiLayout: {
  get: () => apiFetch<GuiLayoutDTO>('/api/gui-layout'),
  update: (data: GuiLayoutDTO) => apiFetch<any>('/api/gui-layout', { method: 'PUT', body: JSON.stringify(data) }),
},
```

### New Frontend Components

#### `GuiLayoutPage.vue` (view)

Full-page layout with:
- Title bar: "GUI Layout" heading + "Apply & Reload" and "Reset" buttons
- Page tabs row (reusable `PageTabs.vue`)
- Main area split horizontally:
  - Left: `ChestGrid.vue` rendering the 6×9 slot matrix
  - Right: `SkillPalette.vue` for searching and dragging skills
- Status bar showing pending changes indicator

#### `ChestGrid.vue`

- Renders a 6-row, 9-column grid of `ChestSlot.vue` components
- Grid appearance mimics Minecraft chest UI: dark slots with beveled borders,
  a title bar at the top showing the `gui.yml` title (MiniMessage-rendered)
- Bottom row reserved for navigation (prev/next page buttons, page label),
  matching the in-game chest GUI layout (slots 45-53)
- Accepts prop `page: GuiPageDTO`
- Emits `update:slots` for any change

#### `ChestSlot.vue`

- Represents a single slot in the chest grid
- Three states:
  - **Empty:** Shows subtle border, no icon
  - **Occupied:** Shows `MinecraftIcon` for the assigned skill
  - **Drag-over highlight:** Shows a green-tinted border during drag hover
- **Drag source:** `dragstart` event with `skillId` data transfer
- **Drop target:** `dragover`/`drop` handlers for receiving drops
  - If slot is occupied and dragged skill is different: swap
  - If slot is occupied and same skill: no-op
  - If slot is empty: assign
- **Hover tooltip:** Shows `SkillTooltip.vue` positioned above the slot
- **Right-click:** Removes the skill from the slot
- **Visual empty slot indicator:** A faint dashed border or ghost icon
- Tagged with `data-slot-index="{index}"` for drag source identification

#### `PageTabs.vue`

- Horizontal row of page tabs (styled as pill-shaped tabs, similar to
  in-game enchantment table navigation)
- Each tab shows the page label (rendered via `minecraftColors.ts`)
- Active tab is highlighted
- Last tab is an "+" button to add a new page
- Each tab has an "✕" to remove (with confirmation dialog)
- Tab labels are editable inline (double-click to edit)

#### `SkillPalette.vue`

- Search input at top with filter icon
- Scrollable list of all registered skills
- Each item shows `MinecraftIcon` + skill display name
- Filtered by search query (matches `id`, `displayName`, ability names)
- Each item is `draggable="true"` with `skillId` in drag data
- Visual feedback on drag: the dragged item gets reduced opacity
- Items are sorted alphabetically by display name

#### `SkillTooltip.vue`

- Reusable tooltip component for skill hover preview
- Renders in the exact same format as in-game lore:
  - Title line: skill display name in gold (`&6`)
  - Separator: grey line (`&7━━━━━━━━━━━━━━`)
  - Lore lines: each line rendered through `parseAmpersandCodes` →
    `renderFormattedText` (same as `renderedLore()` in `AbilitiesSection.vue`)
  - Ability list: each ability name with unlock level
    (`&a{name} &7(Lvl {unlock})`)
  - Footer: skill ID in dark grey (`&8{id}`)

The tooltip positioning:
- Appears above the hovered slot, centered horizontally
- Max-width ~280px with word-wrap
- Background: dark semi-transparent panel (`rgba(0,0,0,0.85)`) with
  Minecraft-style border (`#2a2a2a` outer, `#111` inner)

#### Standardized Tooltip Rendering

The `SkillTooltip.vue` component establishes a reusable pattern for all
tooltips in the web GUI. The existing `minecraftColors.ts` utilities
(`parseAmpersandCodes`, `renderFormattedText`) are used directly, ensuring
pixel-identical rendering to the lore preview in `AbilitiesSection.vue`.

```typescript
// SkillTooltip.vue (conceptual)
const renderedDescription = computed(() =>
  renderFormattedText(parseAmpersandCodes(skill.description || ''))
);
```

This component should be used by:
- `SkillCard.vue` (the dashboard card tooltip on hover)
- `ChestSlot.vue` (the grid slot tooltip on hover)
- `SkillPalette.vue` (the palette item tooltip on hover)

Refactoring existing tooltip usage to `SkillTooltip.vue` is part of Phase 2.

### Backend Handler: `GuiLayoutHandler.java`

```java
package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
import java.util.Map;

public final class GuiLayoutHandler {
    private final StagingManager stagingManager;
    private final File guiLayoutFile;

    private static final String GUI_YML = "gui.yml";

    public GuiLayoutHandler(StagingManager stagingManager, File pluginDir) {
        this.stagingManager = stagingManager;
        this.guiLayoutFile = new File(pluginDir, GUI_YML);
    }

    public void get(Context ctx) {
        // Read gui.yml from disk, parse YAML to Map, return as JSON
        // If file doesn't exist, return default layout
    }

    public void update(Context ctx) {
        // Body is the full GuiLayoutDTO JSON
        // Serialize to YAML, stage to .web_staging/gui.yml
    }
}
```

Registered in `WebServer.java`:

```java
get("/api/gui-layout", layoutHandler::get);
put("/api/gui-layout", layoutHandler::update);
```

### Drag-and-Drop Interaction Model

| Action | Source | Target | Result |
|--------|--------|--------|--------|
| Drag skill from palette → empty slot | `SkillPalette` | `ChestSlot` | Assign skill to slot |
| Drag skill from palette → occupied slot | `SkillPalette` | `ChestSlot` | Assign skill to slot (replaces old) |
| Drag skill from slot → empty slot | `ChestSlot` (drag) | `ChestSlot` (drop) | Move skill to new slot |
| Drag skill from slot → occupied slot | `ChestSlot` (drag) | `ChestSlot` (drop) | Swap skills |
| Drag skill from slot → off-grid (outside `ChestGrid`) | `ChestSlot` (drag) | Any non-grid area | Remove skill from slot (cancel on drop outside = revert) |
| Drag skill from slot → palette "trash" zone | `ChestSlot` (drag) | `SkillPalette` trash area | Remove skill from slot |
| Right-click on occupied slot | `ChestSlot` | — | Context menu: "Remove" |

The implementation uses the HTML5 Drag and Drop API:
- `dragstart`: Set `dataTransfer.setData('text/plain', skillId)` and record source slot index
- `dragenter`/`dragover`: Prevent default to allow drop, add highlight class
- `dragleave`: Remove highlight
- `drop`: Read skill ID from data transfer, determine operation (assign/swap/remove)
- `dragend`: Clean up highlights

For "drag off grid to remove", the `ChestGrid` component listens for `dragend`
on the document. If no valid `drop` occurred, and the source was a grid slot,
the slot is cleared. This is implemented via a flag (`dropReceived`) set in
the `drop` handler and checked in `dragend`.

### Default Layout

If `gui.yml` does not exist, the backend returns a default layout that
matches the current hardcoded behavior:

```yaml
title: "&8⚒ &6Skills &8⚒"
rows: 6
pages:
  - label: "&6Skills"
    slots: {}
```

This ensures backward compatibility — an empty grid renders the same way
as the current hardcoded overview.

### Staging Integration

- `PUT /api/gui-layout` writes the serialized YAML to
  `.web_staging/gui.yml`
- `POST /api/reload` copies it to live `gui.yml` and triggers the plugin's
  lockdown/reload cycle
- `DELETE /api/staging` discards the staged `gui.yml`
- `GET /api/staging/status` shows `gui.yml` as one of the pending files

## Implementation Phases

### Phase 1: Backend API

1. Create `GuiLayoutHandler.java` with `get` and `update` methods
2. Create `GuiLayoutDTO.java` record matching the YAML schema
3. Create `GuiLayoutSerializer.java` for YAML ↔ DTO conversion
4. Register routes in `WebServer.java`
5. Add default layout fallback when file doesn't exist
6. Unit tests for serialization/deserialization
7. Build verification (`./gradlew build`)

### Phase 2: Frontend Foundation

1. Create `stores/gui-layout.ts` Pinia store
2. Add `guiLayout` client methods to `api/client.ts`
3. Create `SkillTooltip.vue` reusable component
4. Refactor existing tooltip usage to `SkillTooltip.vue`
5. Create `ChestSlot.vue` with drag/drop + tooltip
6. Create `ChestGrid.vue` with 6×9 slot rendering, title bar, bottom nav bar
7. Create `PageTabs.vue` with add/rename/remove page
8. Create `SkillPalette.vue` with search and drag source
9. Build verification

### Phase 3: Full Page Integration

1. Create `GuiLayoutPage.vue` view composing all components
2. Add `/layout` route to `router.ts`
3. Add "Layout" nav link to `AppTopbar.vue`
4. Wire store to API (fetch on mount, save on "Apply & Reload")
5. Staging status banner integration
6. Implement "drag off grid to remove" behavior
7. Implement slot swap logic
8. Add empty state (no skills, placeholder text)
9. Add loading skeleton for grid and palette
10. Build verification

### Phase 4: Polish & Testing

1. E2E tests: `GuiLayoutPage` POM in `e2e/pages/`
2. E2E specs: create/rename/remove pages, drag palette → grid, drag grid → grid,
   right-click remove, drag off-grid remove, tooltip visibility
3. Visual regression: tooltip rendering matches lore preview in abilities editor
4. Accessibility: keyboard navigation for grid slots (Tab + Enter to place
   selected skill from palette)
5. Dark mode verification
6. Responsive: grid should be usable down to 900px viewport width
7. Build + test validation (`./gradlew build && npm run e2e`)

## Files Created/Modified

### New files

| File | Responsibility |
|------|---------------|
| `src/main/java/.../web/handler/GuiLayoutHandler.java` | GET/PUT API handler |
| `src/main/java/.../web/dto/GuiLayoutDTO.java` | Record for gui.yml schema |
| `src/main/java/.../web/dto/GuiLayoutSerializer.java` | YAML ↔ DTO conversion |
| `web/frontend/src/views/GuiLayoutPage.vue` | Layout editor view |
| `web/frontend/src/stores/gui-layout.ts` | Pinia store |
| `web/frontend/src/components/layout/ChestGrid.vue` | 6×9 chest grid |
| `web/frontend/src/components/layout/ChestSlot.vue` | Individual slot |
| `web/frontend/src/components/layout/PageTabs.vue` | Page tab strip |
| `web/frontend/src/components/layout/SkillPalette.vue` | Skill search/drag panel |
| `web/frontend/src/components/layout/SkillTooltip.vue` | Reusable lore tooltip |
| `web/frontend/e2e/pages/GuiLayoutPage.ts` | POM for E2E tests |
| `web/frontend/e2e/specs/gui-layout.spec.ts` | E2E test spec |

### Modified files

| File | Change |
|------|--------|
| `src/main/java/.../web/WebServer.java` | Register `GuiLayoutHandler` routes |
| `web/frontend/src/router.ts` | Add `/layout` route |
| `web/frontend/src/components/layout/AppTopbar.vue` | Add "Layout" nav link |
| `web/frontend/src/api/client.ts` | Add `guiLayout` API methods |

## Risk

Low-medium. The editor is entirely additive — no existing functionality is
changed. Risk factors:
- **Drag-and-drop browser inconsistencies:** Mitigated by using native HTML5
  Drag and Drop API with fallback click-to-assign behavior.
- **gui.yml schema changes in the future:** The DTO should use a `version`
  field from the start to allow schema migration.
- **Large skill sets:** The palette supports search/filter; the grid supports
  pagination via the page system.
- **Tooltip rendering divergence:** Using the same `minecraftColors.ts` utilities
  as the existing lore preview ensures pixel-identical rendering.
