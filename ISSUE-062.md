# ISSUE-062: Skills Chest GUI Layout System

## Description

The current skills overview GUI (`SkillMenuBuilder`) places every registered `SkillDefinition` into a flat, linear grid starting at slot 0 and incrementing until the 54-slot chest inventory is full. For the planned 30+ skill web, this layout is unusable: skills appear in arbitrary registration order, there is no categorization, no way to navigate between groups, and server admins have zero control over which skill appears where.

A configurable, paginated, category-based GUI is needed.

## Scope

- New `gui.yml` config file (generated on first run alongside `config.yml` and `tags.yml`)
- Categorization of skills into named pages/groups (e.g., "Gathering", "Combat", "Artisan")
- Pagination: navigation between pages via clickable edge slots (arrows)
- Configurable slot assignment: each skill can be placed in a specific 0–53 slot per page
- Shared decoration slots (edges/filler) configurable per page
- Backward compatibility: if `gui.yml` is absent or empty, fall back to the current flat linear layout
- Caching: page inventories are lazy-built and cached in `PlayerProfile`, invalidated on level change or reload
- Full Poison Pill protection on navigation and decoration items
- No changes to `SkillDefinition`, `SkillManager`, or skill data flow

## Proposed Solution

### `gui.yml` Schema

The file lives at `plugins/Skilling/gui.yml` and is generated on first run with commented defaults.

```yaml
# ============================================================================
# Skills GUI Layout Configuration
# ============================================================================
# Defines how skills are organized into pages and placed in the chest grid.
# If this file is missing or empty, the plugin falls back to the default
# flat sequential layout (current behavior).
#
# Each page renders as a full 54-slot chest inventory (6 rows × 9 columns).
# - Slots 0–53 are valid for skill placement.
# - Navigation arrows are rendered automatically in slots 45 (◀ prev) and 53 (▶ next).
# - Slot 49 shows the current page icon/name.
# - All remaining slots not assigned to a skill or navigation show filler (by default).
# ============================================================================

pages:
  gathering:
    title: "Gathering"
    icon: "minecraft:iron_pickaxe"
    custom_model_data: 1001
    # Slots 0-44 and 46-52 are available for skills.
    # The center column (slot 49) is reserved for page icon by default.
    skills:
      mining: 0
      woodcutting: 1
      digging: 2
      farming: 3
      herbalism: 4
      husbandry: 5

  combat:
    title: "Combat"
    icon: "minecraft:iron_sword"
    custom_model_data: 1002
    skills:
      heavy_weapons: 0
      light_weapons: 1
      unarmed: 2
      archery: 3
      throwing: 4
      one_handed: 5
      dual_wield: 6

  artisan:
    title: "Artisan"
    icon: "minecraft:crafting_table"
    custom_model_data: 1003
    skills:
      smithing: 22
      carpentry: 23
      masonry: 24
      tailoring: 25
      cooking: 40
      building: 41

  arcane:
    title: "Arcane"
    icon: "minecraft:ender_pearl"
    custom_model_data: 1004
    skills:
      alchemy: 0
      enchanting: 1
      piety: 20
      bard: 21
      wizardry: 22

  defense:
    title: "Defense"
    icon: "minecraft:shield"
    custom_model_data: 1005
    skills:
      shields: 0
      heavy_armor: 1
      light_armor: 2
      medium_armor: 3
      unarmored: 4

  mobility:
    title: "Mobility"
    icon: "minecraft:elytra"
    custom_model_data: 1006
    skills:
      riding: 0
      acrobatics: 1
```

#### Schema Reference

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `pages` | map of page-id → page | yes | Ordered map of page definitions. Order in YAML determines navigation order. |
| `pages.<id>.title` | string | yes | Display name shown in the inventory title bar and on the page indicator item. |
| `pages.<id>.icon` | material string | yes | Material for the page indicator item in slot 49 (e.g., `minecraft:iron_pickaxe`). |
| `pages.<id>.custom_model_data` | int | no | Custom model data for resource pack integration on the page icon. |
| `pages.<id>.skills` | map of skill-id → slot | no | Maps a registered skill ID to a chest slot (0–53). Skills not listed are hidden from the GUI. A skill may only appear on one page. |

**Slot allocation rules:**

| Slot range | Purpose |
|------------|---------|
| 0–44 | Skill icon slots (inner 5 rows) |
| 45 | Previous page arrow |
| 46–48 | Available for skills |
| 49 | Page icon / indicator (reserved) |
| 50–52 | Available for skills |
| 53 | Next page arrow |

### How the Chest GUI Renders Pages

1. **Page building.** Each page construction follows this sequence:
   - Create a 54-slot `Inventory` with title `"<page.title>"` (e.g., `Component.text("Combat", NamedTextColor.GOLD)`).
   - Fill all 54 slots with filler glass panes (configurable tint, default `BLACK_STAINED_GLASS_PANE`, custom model data optional).
   - Place page indicator at slot 49: a `BOOK` or the page's `icon` material with the page title as display name.
   - Place navigation arrows:
     - Slot 45: `ARROW` named `"◀ Prev Page"` if this page is not the first.
     - Slot 53: `ARROW` named `"Next Page ▶"` if this page is not the last.
   - Iterate `<page>.skills` map. For each `(skill-id → slot)`:
     - Look up the `SkillDefinition` in `SkillManager`.
     - Call `buildSkillIcon(skill, profile)` (existing method) to produce the `ItemStack`.
     - Place it at the configured slot.
     - If the slot is outside 0–53, log a warning and skip.
     - If the skill-id is not registered in `SkillManager`, log a warning and skip.
   - Tag every item in the inventory with `PoisonPillTag`.
   - Cache the resulting `Inventory` in `PlayerProfile` under a `Map<Integer, Inventory>` keyed by page index.

2. **Navigation.** The existing `UIProtectionListener` already intercepts clicks on `SkillInventoryHolder`. Enhance it to:
   - On click of slot 45 (prev arrow): call `player.openInventory(cachedPage[pageIndex - 1])`.
   - On click of slot 53 (next arrow): call `player.openInventory(cachedPage[pageIndex + 1])`.
   - All other clicks remain cancelled.
   - Navigation clicks do **not** consume the arrow item (the inventory is swapped out entirely).

3. **Page indicator (slot 49).** Shows the current page's `icon` material, named with the page title, and lore listing the skill count. This slot is non-interactive.

4. **Caching.** `PlayerProfile` gets a new field:
   ```java
   private volatile Map<Integer, Inventory> cachedPageInventories;
   ```
   - Built on first menu open by `SkillMenuBuilder`.
   - Invalidated (set to `null`) whenever the player's level changes (existing invalidation hook) or on reload.
   - If `gui.yml` is absent, `cachedPageInventories` remains `null` and `buildOverview` returns the legacy single-page layout cached in the existing field.

5. **`SkillInventoryHolder` enhancement.**
   ```java
   public final class SkillInventoryHolder implements InventoryHolder {
       private final Player player;
       private final int pageIndex;          // -1 for legacy layout
       private final List<String> pageOrder; // ordered page IDs, null for legacy
       // ...
   }
   ```
   This allows the click handler to determine the current page and look up adjacent pages.

### Skills Placed in Specific Slots

The `skills` map in `gui.yml` directly maps `skill-id → slot-number`. This gives admins full control over the exact visual arrangement. There is no auto-placement algorithm — if no slot is defined for a skill, it does not appear on that page.

Validation during `/skills reload`:
- Duplicate slots within the same page → `IllegalArgumentException` (fail-fast).
- Slots 45, 49, 53 cannot be assigned to skills → error if attempted.
- Skill ID not found in `SkillManager` → warning, slot skipped.
- Skill appears on multiple pages → warning, only first occurrence honored.

### Backward Compatibility

If `gui.yml` does not exist or is empty (both `pages` is null or empty), the system behaves exactly as before:
- `PlayerProfile.cachedPageInventories` stays `null`.
- `SkillMenuBuilder.buildOverview(profile)` builds the legacy single 54-slot inventory.
- `SkillInventoryHolder` is constructed with `pageIndex = -1` and `pageOrder = null`.
- The `UIProtectionListener` navigation click handler short-circuits when `pageOrder` is null.

This ensures zero migration cost for existing installations.

### New Files Needed

| File | Responsibility |
|------|---------------|
| `engine/ui/GuiLayoutConfig.java` | Parses `gui.yml` into an immutable config tree; validates slot rules; provides page order, per-page skill→slot map |
| `engine/ui/GuiPage.java` | Record holding a single page's data: `id`, `title`, `icon`, `customModelData`, `Map<String, Integer> skillSlots` |
| `engine/ui/SkillMenuBuilder.java` | **Enhanced** — new `buildPaginatedOverview(profile)` method that iterates `GuiLayoutConfig` pages and builds per-page inventories; caches into `PlayerProfile` |
| `engine/ui/SkillInventoryHolder.java` | **Enhanced** — add `pageIndex`, `pageOrder`, and `pageCount` fields; constructor overloads |
| `engine/ui/UIProtectionListener.java` | **Enhanced** — add navigation click handling for slots 45/53; use `SkillInventoryHolder.pageOrder` to open adjacent pages |
| `engine/profile/PlayerProfile.java` | **Enhanced** — add `cachedPageInventories` field and invalidation method |

### Interaction with Reload Lockdown

The `/skills reload` lockdown sequence (AGENTS.md §6) already calls invalidation of UI caches. The new `cachedPageInventories` field is cleared in step 5 (Invalidate Caches). After unlock, the next `/skills` open will re-parse `gui.yml` and rebuild page inventories.

## Implementation Phases

### Phase 1: Data Model (`GuiPage`, `GuiLayoutConfig`)

1. Create `GuiPage` record with fields: `String id`, `String title`, `String icon`, `int customModelData`, `Map<String, Integer> skillSlots`.
2. Create `GuiLayoutConfig` class that:
   - Reads `gui.yml` from the plugin data folder.
   - Parses the `pages` map into an ordered `List<GuiPage>`.
   - Validates: no reserved-slot collisions (45, 49, 53), no duplicate slots within a page.
   - Logs warnings for unknown skill IDs (does not throw — fail-soft for missing skills).
   - Provides `getPageOrder(): List<String>`, `getPage(String id): GuiPage`, `isPresent(): boolean`.
3. Add registration / instantiation in `Skilling.onEnable()` and reload.
4. Generate default `gui.yml` on first run with an empty `pages:` block and commented instructions.

### Phase 2: Enhanced `SkillMenuBuilder` + `PlayerProfile` Caching

1. Add `cachedPageInventories` field to `PlayerProfile` (`volatile Map<Integer, Inventory>`).
2. Add `invalidatePageCache()` method to `PlayerProfile`.
3. In `SkillMenuBuilder`:
   - If `GuiLayoutConfig.isPresent()`, build paginated inventories:
     - For each page, construct a 54-slot `Inventory` with `SkillInventoryHolder(player, pageIndex, pageOrder)`.
     - Fill filler, place navigation arrows, place page indicator, iterate `skillSlots`.
   - If not present, delegate to existing `buildOverview()`.
4. Cache the `Map<Integer, Inventory>` in `profile`.
5. Hook cache invalidation into level-change path (same spot where existing single-inventory cache is cleared).

### Phase 3: Navigation in `UIProtectionListener`

1. Detect `SkillInventoryHolder` with non-null `pageOrder`.
2. On slot 45 click: if `pageIndex > 0`, open `profile.cachedPageInventories[pageIndex - 1]`.
3. On slot 53 click: if `pageIndex < pageCount - 1`, open `profile.cachedPageInventories[pageIndex + 1]`.
4. All other clicks remain cancelled per existing rules.
5. Cloning the inventory reference on open is safe — the `Inventory` object is already fully built and cached.

### Phase 4: Build, Test, Commit

1. `./gradlew build` — fix any compile errors.
2. `./gradlew test` — ensure existing tests still pass.
3. `git diff` — self-review.
4. Mark issue complete in `ISSUES.md`.
5. `git add -A && git commit -m "feat(ui): add gui.yml layout system with paginated chest GUI"`.

## Out of Scope

- Drag-and-drop GUI editor (the Web GUI will handle this — see `ISSUES.md` line 371).
- Per-player page preferences (page order is global via `gui.yml`).
- Custom slot per player (slot assignments are global).
- More than 6 rows (54-slot chest is the Paper/vanilla limit).

## Risk

Low to medium. The existing flat layout is completely preserved via the `isPresent()` fallback. The new code is isolated in the UI layer with no changes to `SkillManager`, `SkillDefinition`, or skill execution pipelines. The main risk is slot misconfiguration in `gui.yml`, which is mitigated by fail-fast validation at reload time.
