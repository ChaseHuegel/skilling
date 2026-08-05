# REPORT_CUSTOM-ITEMS.md: Supporting Custom Items in Tags & Filters

**Status:** Research report (no production changes applied)
**Issue:** [ISSUE-183](../issues/ISSUE-183.md)
**Date:** 2026-08-02
**Inputs:** `src/main/java/io/github/chasehuegel/skilling/engine/tag/TagResolver.java`, `CustomTagLoader.java`, `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java`, `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java`, `src/main/resources/tags.yml`, `web/frontend/src/components/tags/{TagListEditor,MaterialMultiSelect}.vue`, `docs/reports/REPORT_XP-CURVE.md`, `docs/reports/REPORT_DUALWIELD-API.md`

---

## 1. Executive Summary

Today every Skilling tag, filter, and item requirement resolves to a **flat `EnumSet<Material>`**
pre-computed at load. That gives O(1) event-path matching but **cannot express custom items**:
an item's identity is its material only. A datapack trident, a plugin's "Masterwork Greatsword",
or a renamed, enchanted, or NBT-tagged item is indistinguishable from its vanilla base material.

This report maps the current flow, proposes a **metadata standard** admins apply *to their items*
(so Skilling stays independent of every custom-item solution), proposes **tag-entry syntax
extensions**, and evaluates the **performance model** for item-instance matching that cannot be
pre-flattened. It deliberately explores options and recommends a direction without locking any
in. The guiding constraint: **an admin must be able to read a tag definition and know how it
behaves without a manual.**

---

## 2. Current Flow Map

### 2.1 Resolution & caching

- **`CustomTagLoader`** (`engine/tag/CustomTagLoader.java`) parses `tags.yml` `custom_tags:` into
  `Map<String, EnumSet<Material>>`. Entries are material names (`minecraft:coal`) or tag
  cross-references (`#minecraft:logs`, `#c:ores`). The loader flattens recursively at load and
  throws on unknown materials/tags (fail-fast).
- **`TagResolver`** (`engine/tag/TagResolver.java`) wraps the loader with a
  `Map<String, EnumSet<Material>> resolvedCache`. It supports `#minecraft:<tag>`,
  `#c:<tag>`, and bare `minecraft:<item>` (single-material), and pre-warms the cache at load
  (`SkillManager.validateTagReference` → `warm()`). Event dispatch is an O(1)
  `EnumSet.contains`.
- **`RequirementEngine`** (`engine/requirements/RequirementEngine.java`) does possession/cost
  checks (`countItems`/`removeItems`) against `resolveMaterialSet(tag)`, a material set from the
  same `TagResolver`.

### 2.2 Where material-only matching limits custom items

| Path | Where it matches | Custom-item gap |
|---|---|---|
| `SkillEventListener.matchFilter` (`target:`) | `resolveEventMaterial(event)` → `Material`, then `resolvedSet.contains(material)` | The **event** only exposes a `Material`, never the item instance. For `block_break`/`block_place` that is fine (blocks are materials). For `player_interact`/`consume_item`/`craft_item` the *stack* exists but is discarded down to its `Material`. |
| `SkillEventListener.matchFilter` (`tool:`) | `player.getInventory().getItemInMainHand().getType()` | Drops the held `ItemStack` to a `Material`. |
| `RequirementEngine.countItems/removeItems` | `slotItems(player, slot)` then `resolved.contains(item.getType())` | The full `ItemStack` is available but only its `getType()` is consulted. |
| `equipped_all`/`equipped_any` (armor states) | armor slot `Material` in resolved set | Same: item instance discarded. |

**Conclusion:** every hot path already has the `ItemStack` in hand except the *block* event paths.
The engine flattens to materials for speed. Custom-item support is fundamentally about *not
discarding the instance* in the paths where it exists, plus a way to express instance criteria.

---

## 3. Proposed Metadata Standard

### 3.1 The standard (recommended direction)

Items are matched by **one or more `PersistentDataContainer` (PDC) keys under a reserved
namespace**, e.g. `skilling:tags`. The value is a `List<String>` of tag names. An item "is in"
`#c:my_tag` if `skilling:tags` contains `my_tag` (namespace prefix elided, mirroring `#c:`).

```yaml
# tags.yml: no change needed to reference PDC-tagged items
custom_tags:
  masterwork:
    - "pdc:masterwork"            # NEW entry kind: matches items carrying skilling:tags contains "masterwork"
```

**How admins add the metadata** (Skilling stays solution-agnostic. It only *reads* the key):

- **Commands (`/data`, plugin commands):** `/data merge entity @s ...` or the item-granting
  plugin sets `skilling:tags`. Example via a datapack function:
  ```mcfunction
  give @s minecraft:diamond_sword{custom_model_data:123,skilling:{tags:["masterwork","c:heavy_weapon"]}}
  ```
- **Datapack item components:** `minecraft:custom_data` is a lossless PDC carrier. Any plugin or
  datapack writing `{skilling:{tags:[...]}}` into `custom_data` is matched with zero integration.
- **Plugins (via `SkillingAPI`):** expose a tiny helper `SkillingAPI.tagItem(ItemStack, String...)`
  that writes `skilling:tags` so addons do not hand-roll the key.

**Why PDC:** it is the Paper-native, type-safe, version-portable way to attach data. It survives
saves. It is exactly what `custom_data` maps to. It keeps Skilling's surface a *read-only*
convention rather than an integration API.

### 3.2 Concrete examples by creation method

| Creation method | How the item gets matched |
|---|---|
| Datapack recipe/command | Author includes `{skilling:{tags:["fishing_rod"]}}` in the item `custom_data`. |
| Plugin (custom item lib) | On item creation, plugin calls `SkillingAPI.tagItem(stack, "masterwork")` (or writes the PDC key directly). |
| Vanilla renamed item | `name` entries (below) cover rename-based matching without touching data. |
| Enchanted/potion-flavored items | Optional `ench`/`potion` entry kinds (explored in §4). |

---

## 4. Tag-Entry Syntax Extensions (explored, not locked in)

Current entry kinds: `minecraft:material`, `#minecraft:tag`, `#c:tag`. Proposed additions:

| Entry syntax | Meaning | Readability | Notes |
|---|---|---|---|
| `pdc:<name>` | Item's `skilling:tags` PDC list contains `<name>` | High | Canonical custom-item match. Needs the metadata standard (§3). |
| `name:<pattern>` | Item display name matches (regex or glob) | High | `name:*Greatsword`, `name:Masterwork Sword`. Cheap when cached against a regex `Pattern`. |
| `ench:<enchantment>` | Item carries the enchantment | Medium | e.g. `ench:sharpness`. Requires inspecting `ItemMeta.getEnchants()`. |
| `potion:<effect>` | Item is a potion with the effect | Medium | Niche. Only for `potion` items. |
| `nbt:<path>=<value>` | Raw NBT path match | Low | Expressive but unreadable. **Avoid** as a primary syntax. Keep behind `pdc:`/`name:` where possible. |

**Composition:** entries compose with the existing material/tag entries in the same list. Semantics
should be **OR within a tag** (an item matches `#c:my_tag` if *any* entry matches), consistent with
how material sets already union. This keeps `#c:tools` = {pickaxes, axes, … custom pdc:tool} a
single readable list.

**Validation / fail-fast:** each new entry kind is validated at load:
- `pdc:<name>`: any non-empty name is valid (no compile-time material to check).
- `name:<pattern>`: the regex/glob is compiled at load. A malformed pattern throws
  `IllegalArgumentException`.
- `ench:<id>`: resolved against `Registry.ENCHANTMENT` at load (fail-fast, like materials).

**Not in scope:** block-state / worldgen matching, data-component JSON blobs. Keep it to items.

---

## 5. Performance Model

### 5.1 The core problem

`EnumSet<Material>` pre-flattening gives O(1) event dispatch, but **instance criteria
(`pdc:`, `name:`, `ench:`) cannot be pre-flattened to materials**. Two diamond swords differ.
The engine must fall back to *item-instance* matching on the hot path, which is `O(1)` per tag
but requires touching `ItemMeta` (a PDC read or a `getDisplayName()`), not just `getType()`.

### 5.2 Proposed strategy (two-tier matcher)

Keep the **material tier** exactly as today (a tag with only material/vanilla entries resolves
to `EnumSet<Material>` and never touches item meta). Add an **instance tier**:

1. **Load-time classification.** `TagResolver` inspects each tag's entries. If all entries are
   material/vanilla-tag, the tag stays a pure `EnumSet` (zero hot-path change). If any entry is
   `pdc:`/`name:`/`ench:`, the tag is marked "instance-based" and carries:
   - its material set (still used as a fast pre-filter, if any material entries exist), plus
   - an ordered list of `ItemMatcher` predicates (compiled `Pattern`s, PDC key checks, enchant checks).
2. **Event path.** `matchFilter`/`countItems` first do the material `contains` check. **Only if a
   material entry matches** do they evaluate the instance predicates against the `ItemStack`.
   This means the common case (vanilla material in a mixed tag) still short-circuits, and an
   instance-heavy tag only pays `getItemMeta()` when the material already passed.
3. **Caching the expensive parts.**
   - `name:` regexes are compiled once at load (no per-event `Pattern.compile`).
   - `ItemMeta.getDisplayName()` / PDC reads are inherently per-stack. Bound them by only running
     the instance tier after the material pre-filter, and by checking *cheapest* predicates first
     (PDC key presence before name regex).
4. **Inventory scans (`countItems`/`removeItems`).** These already iterate slot stacks. The
   per-stack cost is one material `contains` plus, for instance tags, one `getItemMeta()` call.
   For a 36-slot hotbar scan that is ≤36 meta reads *only when the tag has instance entries*.
   Pure-material requirements stay free.

**Quantified expectation:** a server with zero custom items sees **zero** hot-path change
(every tag remains `EnumSet`). A server using `pdc:` tags pays one `getItemMeta()` per candidate
stack per event, which is within the engine's 20 TPS budget for reasonable stack counts and is
not re-resolution. The *definitions* are cached. Only the per-stack read is live.

### 5.3 No double-work on chain/harvest paths

`ChainBreakMechanic`/`AreaHarvestMechanic` short-circuit re-dispatch via the `PROCESSING` guard,
so instance-based `target:` filters are evaluated once per origin break, unchanged.

---

## 6. Authoring Ergonomics

### 6.1 Readability goal

A tag definition must be self-explanatory:

```yaml
custom_tags:
  masterwork_gear:      # reads naturally
    - "pdc:masterwork"  # items carrying skilling:tags:["masterwork"]
    - "name:*Greatsword"# or any item named *Greatsword
```

Keep a **one-line comment convention** at the top of `tags.yml` documenting the entry kinds, so
the file is self-describing. Every entry kind is a single token. No nested YAML.

### 6.2 Validation & fail-fast

- Unknown material/tag → already throws at load (unchanged).
- Malformed `name:` pattern → throw at load with the offending pattern.
- Unknown `ench:` id → throw at load.
- A `pdc:` entry is always valid syntactically (the payload is arbitrary).

### 6.3 Web editor UX (`TagListEditor.vue` / `MaterialMultiSelect.vue`)

- The `MaterialMultiSelect` currently offers only materials/tags. Add a **mode toggle** per entry:
  `Material` | `Vanilla tag` | `Custom tag` | **`Custom item (pdc:)`** | **`Name pattern`**.
- For `pdc:` mode, render a free-text input with the existing custom-tag names as a datalist
  (so admins can reference tags they've already defined).
- For `name:` mode, render a text input with a live "matches N items named like this" preview
  hint and regex escaping guidance.
- The API `/api/tags` payload must round-trip the new entry kinds verbatim (they are plain
  strings. The backend already treats tag entries opaquely until `CustomTagLoader` parses them).

---

## 7. Recommendation & Sequencing

**Recommended direction:** `pdc:<name>` as the canonical custom-item entry (backed by the
`skilling:tags` PDC standard), plus `name:<pattern>` as the zero-integration alternative, with
`ench:` deferred until usage demands it. Do **not** ship a generic `nbt:` syntax as a primary
feature.

Suggested follow-up issues (not in this report's scope):

1. **`feat(engine):` two-tier tag resolver.** `ItemMatcher` predicates, load-time
   classification, instance-tier evaluation only after the material pre-filter (§5.2).
2. **`feat(api):` `SkillingAPI.tagItem`** helper writing `skilling:tags` (§3.1).
3. **`feat(web):` TagListEditor entry-kind modes** (§6.3).
4. **`docs(users):` tags.yml entry-kind reference.** One comment block at the top of
   `src/main/resources/tags.yml` and a `capabilities.md` section.
5. **`test(engine):`** unit tests for mixed tags (material pre-filter + instance predicates),
   `name:` regex compilation fail-fast, and PDC-based matching across `matchFilter`,
   `countItems`, and `removeItems`.

---

## 8. Verification of DoD

- [x] Current tag/filter/requirement resolution flow mapped and material-only limitation pinpointed. §2.
- [x] Metadata standard proposed (PDC `skilling:tags`) with concrete examples for commands/datapacks/plugins. §3.
- [x] Tag-entry syntax extensions proposed and justified, with composition and validation. §4.
- [x] Performance model evaluated: two-tier matcher, pre-filter + instance tier, caching. §5.
- [x] Authoring ergonomics addressed: readability, fail-fast, web editor UX. §6.
- [x] No production code changes (research only).

**Cross-reference:** [ISSUE-183](../issues/ISSUE-183.md).
