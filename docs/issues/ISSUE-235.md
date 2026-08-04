# ISSUE-235: Chain/level break off-by-one and durability waste on unbreakable blocks

## Context & User Story
- **Goal:** As a skill author, I want `core:chain_break`/`core:level_break` to honor their documented `chain_limit` (total blocks broken including the origin) and to not burn tool durability when a chained block cannot actually break.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — `chain_limit: N` breaks N+1 blocks, and chaining into unbreakable same-type blocks (e.g. bedrock/obsidian) consumes durability for nothing.

## Implementation Requirements
- [ ] `ChainBreakMechanic.execute` (`ChainBreakMechanic.java:92-117`) counts only chained blocks in `broken` while the Javadoc (`:24`, shared with `LevelBreakMechanic`) says `chain_limit` is "max total blocks broken including the origin". Make the count match the documented semantics (include the origin in the limit) or change the documentation — pick one contract and test it.
- [ ] When `neighbor.breakNaturally(...)` breaks nothing (e.g. an unbreakable same-type block), do not increment `broken` and do not call `ToolDurability.damageOnce` (`:106-111`), so the limit is not consumed and the tool is not worn.
- [ ] Add unit tests for both: exact break count at the limit, and zero durability cost when chained blocks fail to break.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/LevelBreakMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanicTest.java`
- **Dependencies:** `ToolDurability`.
- **Constraints:** The origin block is broken by the originating `BlockBreakEvent`, not by the mechanic; keep that pipeline intact.

## Verification & Definition of Done
- [ ] `chain_limit: N` yields exactly N total blocks (including origin) broken.
- [ ] An unbreakable chained block neither consumes the limit nor the tool durability.
- [ ] `./gradlew build` and `./gradlew test` pass.
