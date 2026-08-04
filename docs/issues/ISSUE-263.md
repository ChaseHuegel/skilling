# ISSUE-263: Unbounded chain_limit / max_blocks can freeze the main thread

## Context & User Story
- **Goal:** As a server admin, I want a large `chain_limit` / `max_blocks` value (or a level-scaled evaluator that reaches one) to be clamped, never to trigger a synchronous multi-block break of hundreds of thousands of blocks that freezes the server.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — main-thread freeze (TPS drop) reachable from config.

## Implementation Requirements
- [x] Clamp the block budget in `ChainBreakMechanic` (`chain_limit`) and `AreaHarvestMechanic` (`max_blocks`) to a sane cap at execution time, and/or validate the range at load via `MechanicParamValidators`.
- [x] Add unit tests asserting oversized values are clamped to the cap.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java:74` (`chain_limit` used raw; registered at `Skilling.java:293` with a param list but no range validator)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AreaHarvestMechanic.java:66` (`max_blocks` never clamped; `radius` is clamped to `MAX_RADIUS` at `:64`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/LevelBreakMechanic.java` (inherits the same `chain_limit` param)
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanicTest.java`, `AreaHarvestMechanicTest.java`
- **Dependencies:** `MechanicParamValidators` as the reference validation pattern.
- **Constraints:** Every sibling mechanic clamps its magnitude params; pick a documented cap consistent with the existing `MAX_RADIUS` (e.g. ≤ 128 blocks).

## Verification & Definition of Done
- [x] `chain_limit`/`max_blocks` above the cap are clamped, not executed raw.
- [x] `./gradlew build` and `./gradlew test` pass.
