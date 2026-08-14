# ISSUE-296: Engine minor fixes — block metadata, event ordering, and mechanic edge cases

## Context & User Story
- **Goal:** As a maintainer, I want a batch of small engine correctness fixes that each have a clear, bounded scope and a regression test.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] **Chained/harvested blocks keep `player_placed` metadata:** `onBlockBreak` returns early for chain/harvest blocks, skipping the `player_placed` metadata cleanup at `SkillEventListener.java:103-116`. `ChainBreakMechanic`/`AreaHarvestMechanic` break neighbors via `breakNaturally`, leaving the marker on the `Block`; a block that regenerates is then falsely treated as player-placed. Clear the metadata for chained blocks too.
- [x] **`fall_damage` abilities fire after a cancel:** `SkillEventListener.java:184-188` dispatches `entity_damage_taken` then `fall_damage` with no `isCancelled()` check between them. Re-check `event.isCancelled()` before the second dispatch.
- [x] **`AutoReplantMechanic` overwrites whatever occupies the spot:** `AutoReplantMechanic.java:36-46` unconditionally `setType(type)` one tick later. Only replant when the block is still `AIR` (and verify the player is still relevant).
- [x] **`SetCooldownMechanic` re-resolves materials and truncates ticks:** `SetCooldownMechanic.java:28-36` runs `Material.matchMaterial(...)` per activation with no cache, and `(int) ticks` truncates level-scaled fractional values. Route through the cached `TagResolver`, round rather than truncate, and clamp to a sane range.
- [x] **`slot: HAND` semantics scan the whole inventory:** `RequirementEngine.java:234-255` maps `HAND`/`ANY`/`ALL` to a full-inventory scan, so a `cost` intended to come from the hand is paid from anywhere. Define `HAND` = main hand only (separate `ANY` for whole-inventory), validate slots at load, and unit-test each equipment slot.
- [x] **`curve: constant` is degenerate:** `SkillManager.java:260` + `LevelThresholds.java:72-87`: any XP ≥ `base_xp` yields max level. Either reject `constant` as a progression curve with a hint to use `linear`, or document its exact semantics in `template-skill.yml`.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:103-116,184-188`, `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AutoReplantMechanic.java:36-46`, `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/SetCooldownMechanic.java:28-36`, `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:234-255`, `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:260`, `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/LevelThresholds.java:72-87`, `src/main/resources/template-skill.yml`.
- **Dependencies:** None.
- **Constraints:** Each fix stays within its own bounded behavior; do not refactor surrounding code. Per `src/AGENTS.md`, add tests for each behavior change.

## Verification & Definition of Done
- [x] Each sub-item has a regression test (one commit per issue per the workflow is acceptable for this batched ticket).
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: existing bundled-skill behavior (which uses `polynomial` curves and standard slots) is unchanged.
