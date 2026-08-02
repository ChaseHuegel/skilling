# ISSUE-181: Replace hard-coded `equipped:*` armor states with data-driven `equipped_all` / `equipped_any` plus bundled tags

## Context & User Story
- **Goal:** As an addon author, I want armor-tier gating expressed entirely in YAML via tags and generic states, so no skill-specific armor knowledge is hard-coded in the plugin backend.
- **Agent Role:** You are an expert backend (Paper) + web (Vue) engineer executing this task.

The current `equipped:none|light|medium|heavy` state filter hard-codes armor tiers into the plugin via material-name substring checks in `ArmorTierMatcher` (`LEATHER`, `CHAINMAIL_`/`IRON_`/`GOLDEN_`/`TURTLE_HELMET`, `DIAMOND_`/`NETHERITE_`). This violates the plugin architecture rule of zero hard-coded skill-specifics. Replace it with two target-driven states and bundled tags.

## Implementation Requirements
- [ ] Add state filter `equipped_all:<target>`: passes when **every** armor slot (helmet, chestplate, leggings, boots) holds an item whose material matches `target` (a direct material like `minecraft:leather_helmet` or a `#...` tag).
- [ ] Add state filter `equipped_any:<target>`: passes when **at least one** armor slot holds an item matching `target`.
- [ ] Route the `target` value through the existing `TagResolver` (material or tag), reusing the cached/pre-warmed resolution.
- [ ] Remove the hard-coded `equipped` state filter and `ArmorTierMatcher`; remove the `armor:empty` state if it becomes unused after migration.
- [ ] Introduce bundled tags in `tags.yml`: `unarmored`, `light_armor`, `medium_armor`, `heavy_armor`. Light/medium/heavy reproduce the current material sets as tag contents. `unarmored` = any wearable non-armor item **including air** (to capture literally empty slots), elytra, pumpkins, and similar.
- [ ] Migrate defensive skills to the new states as their XP source state:
  - `light_armor.yml`, `medium_armor.yml`, `heavy_armor.yml` → `equipped_all:<tag>` (e.g., `equipped_all:#c:heavy_armor`) for XP sources, mechanic filters, and requirement states.
  - `unarmored.yml` → `equipped_all:#c:unarmored` (replacing `armor:empty`).
- [ ] Update the web editor so admins can author the new states: `web/frontend/src/components/common/stateFilters.ts` fallback suggestions, the backend `/api/state-filters` keys (auto-derived from the registry), and any filter/requirement value UX.
- [ ] Update user docs and templates that document the old `equipped` state: `docs/users/configuration.md`, `docs/users/capabilities.md`, `docs/users/creating-skills.md`, and `src/main/resources/template-skill.yml`.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` — `registerBuiltinStateFilters()` (`armor` at line 467, `equipped` at line 482).
  - `src/main/java/io/github/chasehuegel/skilling/engine/ArmorTierMatcher.java` (remove/replace).
  - `src/main/java/io/github/chasehuegel/skilling/engine/registry/StateFilterRegistry.java` (unchanged, reuses `state:value` split at first colon).
  - `src/main/resources/tags.yml`
  - `src/main/resources/skills/{light_armor,medium_armor,heavy_armor,unarmored}.yml`
  - `web/frontend/src/components/common/stateFilters.ts`, `FilterBuilder.vue`, `AbilitiesSection.vue`
  - `src/test/java/io/github/chasehuegel/skilling/engine/StateFilterEquippedTest.java`
- **Dependencies:** `TagResolver` material/tag resolution; `#c:` custom tag loading (`CustomTagLoader`).
- **Constraints:**
  - `equipped_all` with the `unarmored` tag must be **permissive** (any of: empty/AIR, elytra, pumpkin … in every slot), not the strict all-empty check `armor:empty` used today.
  - Keep the O(1) event-path lookups: resolve `equipped_*` targets via the cached `TagResolver` sets, no per-event resolution.
  - The state string is split at the first colon (`state.indexOf(':')`), so `equipped_all:#c:heavy_armor` parses as key `equipped_all`, value `#c:heavy_armor`.
  - No armor-tier knowledge may remain in Java (grep for `LEATHER`/`CHAINMAIL_`/`GOLDEN_`/`DIAMOND_`/`NETHERITE_` substring checks after the change).

## Verification & Definition of Done
- [ ] `./gradlew build` and `./gradlew test` pass
- [ ] No hard-coded armor-tier material matching remains in the Java backend
- [ ] In-game: light/medium/heavy armor skills grant XP only when all slots match the tag; `equipped_any` grants when at least one slot matches; unarmored grants when every slot is empty or a wearable non-armor item
- [ ] Web editor offers `equipped_all:` / `equipped_any:` suggestions and round-trips them into YAML
- [ ] Unit tests for `equipped_all` / `equipped_any` (empty slots, mixed tiers, direct material vs tag target)
