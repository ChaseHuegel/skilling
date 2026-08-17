# ISSUE-307: Building — redstone craft and place XP sources

## Context & User Story
- **Goal:** As a server owner, I want redstone construction rewarded in the Building skill so building contraptions is part of the building loop, without event-based redstone perks.
- **Agent Role:** You are an expert content engineer editing bundled skill YAML and tags.

## Implementation Requirements
- [x] Add a `#c:redstone_components` tag to `src/main/resources/tags/base.yml` covering redstone dust, redstone torch, repeater, comparator, piston, sticky piston, observer, hopper, dispenser, dropper, redstone lamp, lever, daylight detector, target block, note block, crafter, tripwire hook, and rails, with a doc comment.
- [x] Add a Building XP source for crafting redstone components: `craft_item` filtered on `target: "#c:redstone_components"` (~60).
- [x] Add a Building XP source for placing redstone components: `block_place` filtered on `target: "#c:redstone_components"` (~30).
- [x] Do NOT add `note_play` or `craft_crafter` triggers or sources, and do NOT add abilities for redstone.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/building.yml`, `src/main/resources/tags/base.yml`.
- **Dependencies:** Existing `craft_item`/`block_place` triggers and target filters.
- **Constraints:** No raw `BlockRedstoneEvent` hooks (anti-grind and player-attribution risks, per REPORT_SKILL-COVERAGE section 3.2). Reward the act of building contraptions, not powering them.

## Verification & Definition of Done
- [x] Bundled-skill auto-sweeps pass.
- [x] `./gradlew build` and `./gradlew test` pass.
- [x] `docs/users/configuration.md` documents the new tag.