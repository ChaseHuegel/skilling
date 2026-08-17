# ISSUE-306: Excavation — archaeology XP sources

## Context & User Story
- **Goal:** As a server owner, I want archaeology XP in the Excavation skill so brushing suspicious blocks and crafting pottery reward the under-used 1.20 archaeology loop, without adding any new abilities.
- **Agent Role:** You are an expert content engineer editing bundled skill YAML and tags.

## Implementation Requirements
- [ ] Add a `#c:suspicious_blocks` tag to `src/main/resources/tags/base.yml` (`minecraft:suspicious_sand`, `minecraft:suspicious_gravel`) with a doc comment.
- [ ] Add an Excavation XP source for brushing suspicious blocks: `player_interact` filtered on `tool: "minecraft:brush"` and `target` in `#c:suspicious_blocks`, with a notably higher reward than digging (~120) because brushing is an uncommon, slow activity.
- [ ] Add an Excavation XP source for digging out suspicious blocks: `block_break` filtered on `target: "#c:suspicious_blocks"` and `state: "player_placed:false"`, lower reward (~40).
- [ ] Add an Excavation XP source for crafting `minecraft:decorated_pot` (~60).
- [ ] No new abilities are added to Excavation.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/excavation.yml`, `src/main/resources/tags/base.yml`.
- **Dependencies:** Existing `player_interact`/`block_break`/`craft_item` triggers. Brush filtering uses a single-material `tool` filter (`minecraft:brush`); there is no vanilla brush tag.
- **Constraints:** `player_placed:false` so placed-and-re-mined suspicious blocks cannot farm XP. Rewards follow the bundle's uniform-XP conventions.

## Verification & Definition of Done
- [ ] Bundled-skill auto-sweeps pass (`SkillYamlValidationTest`, `BundledTagsAndSkillsConsistencyTest`).
- [ ] `./gradlew build` and `./gradlew test` pass.
- [ ] `docs/users/capabilities.md` and `docs/users/configuration.md` document the new tag if a material/tag reference is user-visible.