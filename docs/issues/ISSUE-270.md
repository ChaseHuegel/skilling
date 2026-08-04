# ISSUE-270: Bundled alchemy abilities pair brew_potion with modify_brew_time and never fire

## Context & User Story
- **Goal:** As a player, I want the `rapid_brewing` and `master_alchemist` abilities to actually reduce brew time when I brew potions.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — two shipped abilities are permanent no-ops because the mechanic only acts on `BrewingStartEvent`, not the `BrewEvent` their trigger listens for.

## Implementation Requirements
- [x] Change `trigger: "brew_potion"` to `trigger: "brew_start"` on the `rapid_brewing` and `master_alchemist` abilities in `src/main/resources/skills/alchemy.yml` (lines 20 and 85).
- [x] Fix the `capabilities.md` entry for `core:modify_brew_time` to document its actual event (`BrewingStartEvent`), aligning with the doc's own trigger table.
- [x] Add a unit test asserting `core:modify_brew_time` executes on a `BrewingStartEvent` (and not on a `BrewEvent`), if not already covered.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/resources/skills/alchemy.yml:20` (`rapid_brewing`), `:85` (`master_alchemist`) — both `trigger: "brew_potion"` + `type: "core:modify_brew_time"`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyBrewTimeMechanic.java:22` (returns `false` unless the event is a `BrewingStartEvent`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/BrewPotionTrigger.java:17` (fires on `BrewEvent`)
  - `docs/users/capabilities.md:231` (documents the wrong event)
  - `src/test/java/io/github/chasehuegel/skilling/mechanic/ModifyBrewTimeMechanicTest.java`
- **Dependencies:** the `brew_start` trigger (`BrewStartTrigger`) must be registered.
- **Constraints:** Keep the XP source on `brew_potion` unchanged (that is a valid pairing). Verify no other bundled skill pairs `brew_potion` with `core:modify_brew_time`.

## Verification & Definition of Done
- [x] Both abilities execute on brewing start.
- [x] No other `brew_potion` + `core:modify_brew_time` pairing remains.
- [x] `./gradlew build` and `./gradlew test` pass.
