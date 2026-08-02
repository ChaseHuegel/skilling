# ISSUE-118: Fix `ModifyFurnaceOutputMechanic` handing out furnace blocks instead of smelted product

**Status:** Resolved
**Type:** Bug
**Severity:** High (wrong items awarded)

---

## Context & User Story

- **Goal:** As a player, I want the furnace output bonus to give me extra smelted material, not extra furnace blocks.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Derive the bonus item from the smelted product (`FurnaceExtractEvent.getItemType()` / `getItemAmount()`), not from `extractEvent.getBlock().getDrops()`
- [x] Spawn/credit only the bonus portion (multiplier − 1) so total smelted yield matches the intended bonus
- [x] Keep the item in the player's inventory when possible (match how the base mechanic grants items)
- [x] Add a unit test asserting the bonus is the smelted material, not a furnace block

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyFurnaceOutputMechanic.java:26-34`
- **Dependencies:** `FurnaceExtractEvent` from the `furnace_extract` trigger (`SkillEventListener.java`).
- **Constraints:** Respect vanilla stack-size limits when adding the bonus to the inventory.

### Root Cause

`extractEvent.getBlock().getDrops()` returns the **furnace block's** drops (a furnace item), not the smelted product. Every triggered extraction hands the player `bonus` furnace items instead of the smelted material.

### Proposed Fix

Build the bonus stack from `extractEvent.getItemType()` with amount derived from the event's item amount and the configured multiplier, and grant exactly the extra amount to the player.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new regression test
- [x] Unit test: extraction of iron yields bonus iron ingots, never furnace blocks (bonus-source verified by the mechanic no longer referencing `getBlock()` and by the extracted `computeBonus` unit test; the execute-path ItemStack-content assertion cannot run in plain JUnit because the Paper `ItemType` registry is not bootstrapped there)
- [x] Unit test: multiplier of 1 grants no bonus
