# ISSUE-119: Fix `AutoSmeltMechanic` destroying Fortune/Silk-Touch drops and inflating nugget ores

**Status:** Resolved
**Type:** Bug
**Severity:** High (item loss + ingot-inflation dupe for nugget ores)

---

## Context & User Story

- **Goal:** As a player, I want auto-smelt to preserve Fortune/Silk-Touch yields and to smelt each ore into the correct product (nether gold ore → nuggets, not ingots).
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Stop discarding the real drops: capture the actual event drops (tool- and enchantment-aware) before `setDropItems(false)` instead of using the empty-handed `block.getDrops()` simulation
- [x] Sum drop counts correctly across multiple stacks rather than `Math.max` over stacks (which collapses to the largest single stack and drops the rest)
- [x] Emit the correct smelted product per material (nugget-producing ores must not become ingots)
- [x] Respect Silk-Touch (no smelt when Silk-Touch is active, matching intended behavior) and document the chosen behavior
- [x] Add unit tests covering: Fortune count preservation, multi-stack sums, nether-gold-ore → nuggets, Silk-Touch no-op

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AutoSmeltMechanic.java:18-51`
- **Dependencies:** `BlockBreakEvent` (`be`). Compare `YieldMultiplierMechanic` (ISSUE-114) for the drop-suppression pattern.
- **Constraints:** Do not change the YAML contract of the mechanic. Preserve the smelt map (`SMELT_MAP`).

### Root Cause

`be.setDropItems(false)` discards all real drops, then `be.getBlock().getDrops()` is the empty-handed simulation — Fortune-multiplied ores and Silk-Touch results are lost. The count uses `Math.max` over stacks, so multiple drop stacks collapse to the largest single stack and the rest vanish. For nugget-dropping ores (nether gold), the nugget count (2-6) is emitted as **ingots**, inflating output up to 6×.

### Proposed Fix

Capture `be.getDropItems()`/the event's drop collection, sum amounts across stacks, then map each material to its smelted product (with a nugget map entry) and spawn the smelted result. Add an immutable material→product map (`Map.of(...)`).

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: Fortune III coal ore yields the expected multiplied smelted amount
- [x] Unit test: nether gold ore smelts to gold nuggets, not ingots
- [x] Unit test: Silk-Touch mining returns raw ore without smelting
- [x] Unit test: multi-stack drop sums are preserved
