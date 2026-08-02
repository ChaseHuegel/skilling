# ISSUE-185: Add a `level_break` chain-break variant that only chains on the XZ plane

## Context & User Story
- **Goal:** As an addon author, I want a chain-break mechanic that breaks connected blocks in the same horizontal layer only, so vein/strip mining never propagates up or down the Y axis.
- **Agent Role:** You are an expert backend (Paper) engineer executing this task.

`core:chain_break` chains in all six directions (including up/down). Introduce a `level_break` variant that chains only on the XZ plane (four horizontal directions) and never moves along Y.

## Implementation Requirements
- [x] Add a `core:level_break` mechanic that reuses the chain logic but restricts expansion to the four XZ directions (`{±1,0,0}`, `{0,0,±1}`), never `{0,±1,0}`.
- [x] Prefer refactoring `ChainBreakMechanic` so the direction set is shared/configurable rather than duplicating the BFS, tool-damage, and guard logic.
- [x] Register `core:level_break` in `MechanicRegistry` (via `Skilling.registerBuiltinMechanics()`) with the same params as `core:chain_break` (e.g., `chain_limit`).
- [x] Share the chain-processing guard (`isChainProcessing` / `PROCESSING`) so chained blocks skip XP/ability re-processing for both mechanics.
- [x] Preserve behavior parity with `core:chain_break`: per-block tool durability cost, `chain_limit` cap, cancel-respect on the synthetic `BlockBreakEvent`.
- [x] Update `template-skill.yml` and `docs/users/capabilities.md` with the new mechanic.
- [x] Add unit tests: XZ-only chaining never breaks blocks above/below the origin; `chain_limit` respected; guard behavior matches `chain_break`.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java` — `DIRECTIONS` at line 21
  - New `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/LevelBreakMechanic.java` (or refactor the shared logic)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` — `registerBuiltinMechanics()` (~line 271)
  - `src/main/resources/template-skill.yml`, `docs/users/capabilities.md`
  - `src/test/**` — mirror existing `ChainBreakMechanic` tests
- **Dependencies:** none (self-contained engine mechanic).
- **Constraints:**
  - The `CHAINING_PLAYERS` set and `PROCESSING` ThreadLocal must be shared so both mechanics cooperate correctly on the same event.
  - Must not re-grant XP/abilities for chained blocks (the guard in `SkillEventListener.onBlockBreak` covers both).
  - Follow the same fail-fast param validation as `core:chain_break`.

## Verification & Definition of Done
- [x] `./gradlew build` and `./gradlew test` pass
- [x] Unit: a vertical stack of the same material is untouched by `level_break`; horizontal neighbors chain; `chain_limit` caps the total
- [x] Manual: breaking a block with `core:level_break` clears the layer but not columns above/below
- [x] No double-XP/double-ability for chained blocks
