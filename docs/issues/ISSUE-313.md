# ISSUE-313: New bundled skill — Exploration

## Context & User Story
- **Goal:** As a server owner, I want an Exploration skill that rewards maps, cartography, scavenging, trial-chamber vaults, and recipe discovery, with a level-1 speed buff that scales aggressively enough to become a consistent (but weak) travel buff.
- **Agent Role:** You are an expert game designer and Java/Paper engineer. Engine triggers `map_fill`, `cartography`, `loot`, `vault_change`, and `recipe_discover` come from ISSUE-305.

## Implementation Requirements
- [ ] Create `src/main/resources/skills/exploration.yml` (id `exploration`, icon `minecraft:map` or `minecraft:cartography_table`, 6-tier milestone template, polynomial curve) and add it to the `bundledSkills` array and the default `gui.yml`.
- [ ] XP sources (do NOT use `chunk_load`; Survival owns it):
  - `map_fill` trigger (`PlayerMapFilledEvent`) (~40)
  - `cartography` trigger (`CartographyItemEvent`) (~60)
  - `loot` trigger (`LootGenerateEvent`, nearby dispatch) (~60)
  - `vault_change` trigger (`VaultChangeStateEvent`) (~80)
  - `recipe_discover` trigger (`PlayerRecipeDiscoverEvent`) (~40)
- [ ] Draft abilities:
  - L1 **Pathfinder** (passive): exploring a new chunk (`chunk_load` ability trigger) grants `core:speed_bonus` with a weak multiplier (1.05 to 1.15) whose duration scales aggressively with level (e.g., 20s rising to 120s+), so it reads as a consistent, weak travel buff while exploring. The `chunk_load` XP source is NOT added; only this ability uses the trigger.
  - L15 **Cartographer's Eye** (passive): `cartography` grants a short `core:xp_bonus`.
  - L25 **Mark the Trail** (active): Sneak + Right-Click with a filled map in hand grants nearby allies a `core:field_aura` speed/leaping buff for a duration (map as the catalyst, cost + cooldown).
  - L50 **Scavenger** (passive): `loot` grants a short `core:xp_bonus`.
  - L75 **Vault Raider** (passive): `vault_change` grants `core:field_aura` Absorption (radius 0), mirroring Survival's Well Rested.
  - L100 **True North** (passive, confirm with design owner): `map_fill` grants a strong, long `core:speed_bonus` plus a short `core:modify_jump` (or an agreed capstone).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/resources/skills/exploration.yml` (new)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (bundledSkills)
  - `src/main/resources/gui.yml`
- **Dependencies:** ISSUE-305 triggers. Existing mechanics (`core:speed_bonus`, `core:xp_bonus`, `core:field_aura`, `core:modify_jump`).
- **Constraints:** `chunk_load` is an ability trigger only, never an XP source. `loot` dispatches to nearby players (two players looting the same chest both gain XP, acceptable for group play). Keep the L1 multiplier weak so the buff is quality-of-life, not a speed meta. Confirm the L100 capstone design with the design owner.

## Verification & Definition of Done
- [ ] Design owner confirms the L100 capstone and the final ability set.
- [ ] Bundled-skill auto-sweeps pass; `exploration.yml` parses and is shipped in `bundledSkills` and `gui.yml`.
- [ ] Lore placeholder tests pass for `{multiplier}` / `{duration}` / `{radius}`.
- [ ] `docs/users/creating-skills.md` shows the exploration sources.
- [ ] `./gradlew build` and `./gradlew test` pass.