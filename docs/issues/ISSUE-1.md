# ISSUE-1: Stealth skill with sneak speed, silent steps, backstab, pickpocket, dark vision, and invisibility cloak

## Context & User Story
- **Goal:** As a server owner, I want a stealth skill so that sneaking becomes a useful PvE tool instead of a "do not fall off a ledge" convenience, with a full 0-100 progression through six milestones.
- **Agent Role:** You are an expert backend engineer implementing the engine modules and the bundled `stealth` skill.

## Implementation Requirements
- [x] Add a `target_unaware` state filter: an `entity_damage` backstab only applies to a hostile `Mob` that is not currently targeting the player.
- [x] Add a `core:sneak_speed` mechanic: a movement-speed bonus that is applied on sneak start and stripped on sneak release, with no per-tick task and no lingering buff.
- [x] Add a generic `core:cancel_event` mechanic (optional percentage `chance`) that cancels the triggering event.
- [x] Add a `core:drop_loot` mechanic: roll a referenced loot table (vanilla or datapack/plugin) and drop the result naturally at the target; players are valid targets and nothing is removed from them.
- [x] Add `physical_interaction`, `sensed`, and combined `trip_trap` triggers, and wire dispatch for all three in `SkillEventListener`.
- [x] Add `#c:pressure_plates`, `#c:sculk_sensors`, `#c:trip_traps`, `#c:humanoid`, and the disjoint `#c:pickpocket_*` entity tags to `tags/base.yml`.
- [x] Author the bundled `stealth.yml` skill (0-100, six milestones) and the per-type pickpocket loot-table datapack under `run/world/datapacks/stealth/`.
- [x] Register all new mechanics, triggers, and state filter in `Skilling.registerBuiltin*`.
- [x] Extend the event-to-material target resolver so `target:` filters match `PlayerInteractEvent` PHYSICAL and `BlockReceiveGameEvent`.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/Skilling.java`, `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java`, `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{SneakSpeedMechanic,CancelEventMechanic,DropLootMechanic}.java`, `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/{PhysicalInteractionTrigger,SensedTrigger,TripTrapTrigger}.java`, `src/main/resources/tags/base.yml`, `src/main/resources/skills/stealth.yml`, `run/world/datapacks/stealth/**`, tests, and `docs/users/capabilities.md`.
- **Dependencies:** Paper API 1.21.8; uses the cancellable `BlockReceiveGameEvent` for sculpt suppression (no `VibrateEvent` exists in this API) and `Action.PHYSICAL` for pressure plates/tripwires (includes tripwires per the API javadoc).
- **Constraints:** No per-tick tasks; no hardcoded skills/abilities (all YAML plus reusable modules); vanilla-restraint on loot tables.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including new unit tests for `core:sneak_speed`, `core:cancel_event`, `core:drop_loot`, the `target_unaware` filter, `BundledTagsAndSkillsConsistencyTest`, and `StealthSkillYamlTest`.
- [x] Edge case handled: `target_unaware` fails closed for non-mob victims; `core:cancel_event` returns false for non-cancellable events; `core:sneak_speed` never leaves the modifier after release; `core:drop_loot` fails closed when the table or target is missing.
- [x] Docs updated: `docs/users/capabilities.md` lists the three triggers, three mechanics, and `target_unaware` filter.
