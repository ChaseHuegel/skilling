# REPORT_SEARCHABLE-LISTS.md: Centralized Searchable Lists

**Status:** Research report (no production changes applied)
**Issue:** [ISSUE-276](../issues/ISSUE-276.md)
**Date:** 2026-08-08
**Inputs:** `web/frontend/src/assets/materials.json`, `web/frontend/src/views/{SkillEditorPage,TagsPage}.vue`, `web/frontend/src/components/common/{SoundConfigEditor,MaterialPicker,AppCombobox,stateFilters}.vue`, `web/frontend/src/components/skills/AbilitiesSection.vue`, `web/frontend/src/components/tags/MaterialMultiSelect.vue`, `web/frontend/src/components/common/MinecraftIcon.vue`, `web/frontend/src/stores/registries.ts`, `web/frontend/src/api/client.ts`, `src/main/java/io/github/chasehuegel/skilling/Skilling.java`, `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java`, `src/main/java/io/github/chasehuegel/skilling/web/handler/StateFilterHandler.java`, `src/main/java/io/github/chasehuegel/skilling/engine/tag/{TagResolver,EntityTagResolver}.java`

---

## 1. Executive Summary

Every editor control with a searchable recommendation list today maintains its
own copy of data. Materials exist in two divergent lists. Tag suggestions exist
in two copies. Sounds cover a fraction of the vanilla set. Particles are one
short list. There is no entity-type list at all. The lists drift because they
have no single owner.

This report inventories every list and rates completeness. It compares three
data sources: hardcoded constants, backend-enumerated Bukkit registries, and
online-sourced data. It recommends a hybrid. The backend enumerates exhaustive
lists from live Bukkit `Registry` objects. The frontend caches them in one
Pinia registry store with an offline fallback. The GUI keeps a small curated
shortlist on top for quality, because an exhaustive raw list is a poor
recommendation list.

The recommendation is a phased plan. The first phase centralizes the frontend
store and constants. The second phase adds the backend enumeration endpoints.
The third phase moves each control to the shared source.

---

## 2. Completeness Audit

| Control | File | List | Size | Source | Completeness |
|---|---|---|---|---|---|
| Icon picker | `components/common/MaterialPicker.vue` | `materials.json` | 407 | curated JSON asset | Partial. About 407 curated items |
| Skill editor tags | `views/SkillEditorPage.vue` | `TAG_SUGGESTIONS_BASE` + `MATERIAL_SUGGESTIONS` | ~14 + ~130 | inline constants | Partial. Divergent from materials.json |
| Tags page tags | `views/TagsPage.vue` | `materialSuggestions` | ~14 | inline constants | Duplicate of SkillEditorPage tags |
| Sounds | `components/common/SoundConfigEditor.vue` | `SOUND_SUGGESTIONS` | ~58 | inline constants | Poor. About 58 of ~1,100 sound events |
| Particles | `components/skills/AbilitiesSection.vue` | `PARTICLE_SUGGESTIONS` | ~49 | inline constants | Partial. A curated subset |
| Entities | none | none | 0 | absent | Missing entirely |
| State filters | `components/common/stateFilters.ts` | `STATE_SUGGESTIONS` | ~30 | shared constant module | Good. Single shared source |
| Mechanics/triggers/state filters (live) | `stores/registries.ts` | mechanic keys, params, triggers, state filters | live | backend `/api/*` | Good. Registry-derived |

Confirmed drift: the material list in `SkillEditorPage.vue`
(`MATERIAL_SUGGESTIONS`) differs from `materials.json`. The tag list in
`TagsPage.vue` duplicates `TAG_SUGGESTIONS_BASE` from `SkillEditorPage.vue`. The
two would drift further without a shared owner.

The one good pattern already exists: `stateFilters.ts` is a single shared
constant module used by both `FilterBuilder.vue` and `AbilitiesSection.vue`.
The report models the fix on this pattern.

---

## 3. Proposed Centralized Architecture

### 3.1 One Pinia registry store

Extend `stores/registries.ts`. It already fetches mechanics, triggers, and
state filters with a `loaded` guard and an offline fallback. Add materials,
tags, entities, sounds, and particles to the same store.

Shape:

```ts
const materials = ref<string[]>([])
const tags = ref<string[]>([])
const entities = ref<string[]>([])
const sounds = ref<string[]>([])
const particles = ref<string[]>([])
```

`fetch()` gains the new endpoints. The `loaded` guard fetches them once per
session. The offline fallback keeps the GUI usable when the API is down.

### 3.2 Shared fallback constants

Move each inline fallback into one module, modeled on `stateFilters.ts`. Create
`web/frontend/src/components/common/recommendedLists.ts`. It exports the
curated shortlists that mirror today's inline constants. The store merges the
backend list with the curated shortlist when building the recommendation list.

The key rule: one copy of each fallback constant. The current hand-duplicated
fallbacks in `SkillEditorPage.vue` and `TagsPage.vue` collapse into one export.

### 3.3 Controls consume the shared source

Refactor each control to read from the store:

| Control | Current source | New source |
|---|---|---|
| `MaterialPicker.vue` | imports `materials.json` directly | store materials |
| `SkillEditorPage.vue` | inline `MATERIAL_SUGGESTIONS` | store tags + materials |
| `TagsPage.vue` | inline suggestions | store tags |
| `SoundConfigEditor.vue` | inline `SOUND_SUGGESTIONS` | store sounds |
| `AbilitiesSection.vue` | inline `PARTICLE_SUGGESTIONS` | store particles |
| `FilterBuilder.vue` | `stateFilters.ts` | unchanged (already shared) |

`AppCombobox` and `MaterialMultiSelect` already accept a `suggestions` prop.
They need no structural change. `MaterialPicker.vue` needs its internal import
replaced with a store read.

---

## 4. Data Source Comparison

### 4.1 Option A: Hardcoded, manually maintained

Today's state. Cheapest to build. Always drifts. A Minecraft release adds
materials, sound events, and particles. No one updates the lists by hand. The
report rejects this as the sole source.

### 4.2 Option B: Backend-enumerated Bukkit registries

The backend enumerates exhaustive lists from live Bukkit `Registry` objects.
Precedent exists:

- `Skilling.registerBuiltins()` already wires `Registry.POTION_EFFECT_TYPE`,
  `Registry.ATTRIBUTE`, `Registry.SOUND_EVENT`, and
  `Registry.PARTICLE_TYPE`.
- `/api/mechanics`, `/api/triggers`, and `/api/state-filters` already serve
  registry-derived data through `StateFilterHandler`.
- `TagResolver` already resolves vanilla tags via `Bukkit.getTag`.

Proposed endpoints:

| Endpoint | Source | Notes |
|---|---|---|
| `GET /api/materials` | `Registry.MATERIAL` | Filter to items/blocks that exist in game |
| `GET /api/sounds` | `Registry.SOUND_EVENT` | ~1,100 keys |
| `GET /api/particles` | `Registry.PARTICLE_TYPE` | ~120 keys |
| `GET /api/entities` | `Registry.ENTITY_TYPE` | Filter out non-spawnable markers |
| `GET /api/tags/all` | `Bukkit.getTag` for block/item registries | Vanilla tag keys |

The lists match the running server's Minecraft version exactly. No version
drift. This is the strongest argument for Option B. The cost is a small backend
handler and a JSON round trip, cached by the frontend store.

Caveat: an exhaustive raw list is a poor recommendation list. The raw material
registry includes blocks like `minecraft:air` and `minecraft:barrier`. A raw
sound list is ~1,100 items. The UI needs the curated shortlist on top. The
report keeps both: backend exhaustive list for coverage, curated shortlist for
quality.

### 4.3 Option C: Online-sourced

The item textures use `raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/...`.
The same repo or a Mojang registry dump could supply name lists.

Pros: one canonical source, no backend work.
Cons: requires a network call per GUI session, version pinning, caching, and
offline behavior. The GUI is a local admin tool. A network dependency for basic
suggestion lists is fragile. The texture CDN has a graceful fallback (a letter
badge). A name list cannot degrade as cleanly. If the CDN is down, the lists
are empty.

The report rejects Option C as the primary source. It notes the CDN version
pinning (`VITE_MINECRAFT_ASSETS_VERSION`) as a useful idea for the backend
enumeration if a future server needs lists for a different version than the one
it runs.

### 4.4 Recommendation

Option B (backend enumeration) as the exhaustive source, with the curated
shortlist from Option A merged on top. The store builds the final list as:

```
recommended = unique(backendList, curatedShortlist)
```

This gives complete coverage and good quality. It matches the running server's
version. The only drift is the curated shortlist, which is small and updated
intentionally.

---

## 5. Proposed API Shape

All endpoints follow the existing registry-derived pattern:

```json
{ "materials": ["minecraft:iron_pickaxe", "..."] }
{ "sounds": ["minecraft:block.note_block.harp", "..."] }
{ "particles": ["minecraft:flame", "..."] }
{ "entities": ["minecraft:zombie", "..."] }
{ "tags": ["minecraft:logs", "..."] }
```

One handler `RecommendedListsHandler` (modeled on `StateFilterHandler`)
produces all five. It reads the live registries and the tag resolver. It takes
a short read lock where it touches shared registries, matching the
`SkillHandler` pattern for reload safety.

The frontend `client.ts` gains:

```ts
recommended: {
    materials: () => apiFetch<{ materials: string[] }>('/api/materials'),
    sounds: () => apiFetch<{ sounds: string[] }>('/api/sounds'),
    particles: () => apiFetch<{ particles: string[] }>('/api/particles'),
    entities: () => apiFetch<{ entities: string[] }>('/api/entities'),
    tags: () => apiFetch<{ tags: string[] }>('/api/tags/all'),
},
```

The store fetches these in the existing `Promise.all` in `fetch()`. The whole
fetch stays one request wave.

---

## 6. Performance and Version Drift

### 6.1 Caching

The store fetches once per session (`loaded` guard). The lists are static for
the lifetime of a server process. A reload rebuilds the registries and the store
can refetch. No per-keystroke fetch. The lists are small (a few thousand
strings at most), so one JSON round trip is negligible.

### 6.2 Version drift

Backend enumeration matches the running server's version exactly. There is no
drift between the lists and the game. This is the decisive advantage over the
current hardcoded lists, which drift every Minecraft release.

The curated shortlist may name a material that a future version renames or
removes. The combobox controls accept free text, so a stale suggestion only
means a missing autocomplete entry, never a broken value. The backend
validators reject a genuinely invalid string at load. The drift is contained.

### 6.3 Server on a different version than the lists

Because the lists come from the live server, this case disappears. The frontend
never assumes a Minecraft version. The texture CDN version stays a separate
visual concern pinned in `.env`.

---

## 7. Phased Implementation Plan

### Phase 1: Centralize the frontend store and fallbacks

1. Extend `stores/registries.ts` with the five new list refs.
2. Create `recommendedLists.ts` with the curated shortlists.
3. Refactor `SkillEditorPage.vue` and `TagsPage.vue` to read the store.
4. Refactor `SoundConfigEditor.vue`, `AbilitiesSection.vue`, and
   `MaterialPicker.vue` to read the store.
5. Keep the backend endpoints out of scope for this phase. The store falls
   back to the curated lists, so behavior is unchanged.

Deliverable: one source of truth in the store, curated fallbacks single-sourced.

### Phase 2: Backend enumeration

1. Add `RecommendedListsHandler`.
2. Register the five routes in `WebServer.java`.
3. Add the `api.recommended.*` client functions.
4. Wire the store to fetch the endpoints and merge with the curated lists.
5. Add unit tests for the handler against mocked registries.

Deliverable: exhaustive, version-correct lists served to the GUI.

### Phase 3: Quality pass

1. Review the merged lists. Remove non-player-facing entries where useful.
2. Add entity suggestions to the controls that reference `target_type`.
3. Confirm every control draws from the store.
4. Run the frontend build and E2E suite.

Deliverable: complete, consistent suggestions across the whole GUI.

---

## 8. Alternative Considered

Alternative: keep the backend out and build one exhaustive JSON asset per
Minecraft version in the frontend, generated at build time.

Pros: no backend change. Cons: the asset must be generated and pinned per
version. It cannot match a server running a different version. It is the same
drift problem as today, just centralized.

The report rejects this because Option B removes the drift entirely at the cost
of a small handler. The GUI is served by the same plugin that owns the
registries. There is no reason to duplicate the version data in a static asset.

---

## 9. Open Questions

1. Should the backend filter the material list to block materials and
   item materials only, or include every `Registry.MATERIAL` entry?
2. Should the sound list exclude legacy or unused sound events, or serve the
   full registry?
3. Should the entity list filter out non-spawnable markers such as
   `minecraft:marker` and `minecraft:area_effect_cloud`?
4. Should the curated shortlist live in the backend or the frontend? The
   report recommends the frontend, so a server owner can edit it without a
   Java rebuild.
5. Should the tag list include item-only, block-only, or both registries'
   vanilla tags?

---

## 10. Recommended Architecture Summary

- Source: live Bukkit `Registry` objects enumerated by a
  `RecommendedListsHandler`.
- Transport: one JSON request wave via `/api/materials`, `/api/sounds`,
  `/api/particles`, `/api/entities`, `/api/tags/all`.
- Cache: one Pinia store fetch per session with a `loaded` guard.
- Quality: curated shortlists merged on top of the exhaustive lists.
- Single source: all controls read the store; no inline constant lists remain.
- Version correctness: lists always match the running server.
