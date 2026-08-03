# ISSUE-196: Correct tool durability handling in block-break mechanics

## Context & User Story
- **Goal:** As a player, I want chain-break and area-harvest to cost the right amount of tool durability (respecting Unbreaking, breaking at max durability, and not letting a tool take 0 durability while still breaking many blocks).
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** Code review of `ChainBreakMechanic` and `AreaHarvestMechanic`.

## Implementation Requirements
- [x] Fix `ChainBreakMechanic.damageTool`: the current `+1` per block ignores the `Unbreaking` enchantment, can push `Damage` past the item's max durability (item can break / desync), and should route through a durability-aware path.
- [x] Add tool-damage accounting to `AreaHarvestMechanic` (it currently breaks every harvested block with no durability cost at all — inconsistent with `ChainBreakMechanic`).
- [x] Ensure a tool reaching max durability breaks (or otherwise behaves like a vanilla break) rather than remaining in an invalid damage state.
- [x] Consider the interaction with the origin break already consumed by vanilla (each mechanic should cost for the *additional* blocks it breaks, mirroring vanilla semantics).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AreaHarvestMechanic.java`
- **Dependencies:** none.
- **Constraints:** Keep the shared logic in one place (ChainBreak already exposes a shared BFS/damage hook to `LevelBreakMechanic`). Respect `ItemDamageEvent` semantics where practical (fire a cancellable damage event so other plugins can veto) instead of mutating `Damageable` directly.
- **Note (resolution):** Shared cost moved to a new `ToolDurability` helper used by both mechanics. `damageOnce` fires a cancellable `PlayerItemDamageEvent` (plugin veto), rolls Unbreaking (1/(level+1) chance to consume per point, read from the enchantment map by key so no registry is needed), and breaks the tool via `setAmount(0)` at max durability instead of exceeding it. Random source and Unbreaking reader are injectable seams for deterministic tests.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New/updated tests assert durability cost per broken block, Unbreaking-chance interaction, and that a tool does not exceed max durability for `ChainBreakMechanic` and `AreaHarvestMechanic`.
