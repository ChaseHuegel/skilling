# ISSUE-303: Bundled Survival skill (max hearts, environmental resilience, campfire aura through-line)

## Context & User Story
- **Goal:** As a server owner, I want a bundled Survival skill that grants permanent max hearts on leveling, earns XP from environmental hazards and wilderness activities, and rewards a campfire "stoke" through-line (L25/L75/L100) that grows into a lesser beacon, plus a bed-sleeping ability, so the skill delivers a survival/camping fantasy without overlapping Cooking or the armor skills.
- **Agent Role:** You are an expert content/Java-Paper engineer authoring bundled skill YAML and its docs. Engine prerequisites are ISSUE-302.

## Implementation Requirements
- [x] Add `src/main/resources/skills/survival.yml`: `id: survival`, icon `minecraft:campfire`, 6-tier abilities (L1/L15/L25/L50/L75/L100), polynomial curve (base_xp 50, exponent 2.5).
- [x] Abilities:
  - L1 **Wildborn**: `core:persistent_attribute` on `minecraft:max_health`, linear `base 0 / step 0.25 / max 25`, bound to `level_up`.
  - L15 **Endure**: `core:cancel_damage` chances on `cause:drowning` (10→40%), `cause:suffocation` (10→40%), `cause:burn` (10→50%).
  - L25 **Stoke** (active): `right_click_block` on `#minecraft:campfires`, costs 1 coal + exhaustion, cooldown 20→10s, `core:field_aura` Regen (targets allies, radius 4→10).
  - L50 **Well-Rested** (passive): `sleep` trigger, `core:field_aura` Absorption (radius 0, amp 0→1, duration 120→480s) on waking plus short Regen.
  - L75 **Kindling**: same stoke (coal held, synced cooldown, no cost), `core:field_aura` Absorption aura (radius 6→12).
  - L100 **Lesser Beacon**: same stoke (synced cooldown), `core:field_aura` Speed I + Haste I + Resistance I (radius 8→16).
- [x] XP sources: `entity_damage_taken` on `cause:burn` (×2, damage-scaled ~14), `cause:drowning` (~18), `cause:suffocation` (~18); `player_interact` on `#minecraft:campfires` with `#c:campfire_foods` (~60); `sprint` + `is_in_water` (~30–60); `chunk_load` (~40–60); `sleep` (~60–100); `craft_item` on `#c:campfires` (~60).
- [x] Add `#c:campfire_foods` and `#c:campfires` (item-form) tags to `src/main/resources/tags/base.yml` with doc comments.
- [x] Add `survival.yml` to the `bundledSkills` array in `Skilling.onEnable`.
- [x] Player-facing lore obeys the `&7Costs`/`&8Requires` convention for requirement-bearing abilities; field_aura lore uses `{radius}`/`{duration}`/`{amplifier}` sub-scale placeholders.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/resources/skills/survival.yml` (new)
  - `src/main/resources/tags/base.yml`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
- **Dependencies:** ISSUE-302 (persistent_attribute, cause filter, chunk_load, sleep). Existing `core:cancel_damage`, `core:field_aura`.
- **Constraints:** Skipping documented keys and defaults per `docs/dev/template-skill.yml`. Vanilla-restraint design pillars; no food-buff overlap with Cooking; ability numbers follow the 6-tier milestone template and dual-layer sub-scaling. `docs/users/` updates in the same change.

## Verification & Definition of Done
- [x] Bundled-skill auto-sweeps pass (`SkillYamlValidationTest`, `BundledTagsAndSkillsConsistencyTest`): survival.yml parses, tags/triggers/filters resolve.
- [x] `./gradlew build` and `./gradlew test` pass.
- [x] Docs updated: `docs/users/capabilities.md` (persistent_attribute, `cause` filter, chunk_load/sleep triggers), `docs/dev/SKILL-DESIGN-FRAMEWORK.md` (persistent-modifier note), `docs/dev/template-skill.yml` + `docs/users/creating-skills.md` (examples). STE prose.
- [x] Ability numbers are level-scaled; every milestone past L1 has at least one sub-scaling parameter.