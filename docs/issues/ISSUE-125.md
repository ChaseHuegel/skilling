# ISSUE-125: Fix `ModifyCraftOutputMechanic` shift-click craft duplication

**Status:** Resolved
**Type:** Bug
**Severity:** High (extra items beyond recipe-expected output)

---

## Context & User Story

- **Goal:** As a player, I want craft-output bonuses to produce the intended extra amount whether I craft normally or shift-click, never more than the configured multiplier.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Handle `isShiftClick()` explicitly: the current item amount already reflects the batch-scaled total, so the bonus must be computed against the underlying recipe count, not added on top of the batch total
- [x] Cap the final stack at the material's max stack size (respect `Material.getMaxStackSize()`)
- [x] Add unit tests covering: normal craft, shift-click batch craft, stack-size cap

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyCraftOutputMechanic.java:23-32`
- **Dependencies:** `CraftItemEvent` from the `craft_item` trigger.
- **Constraints:** Do not change the YAML contract (`multiplier`).

### Root Cause

`craftEvent.getCurrentItem()` for a shift-click already carries the batch-scaled amount. The mechanic adds `bonus = round(amount * (multiplier − 1))` on top of that batch total without distinguishing `isShiftClick()`, producing extra items beyond the recipe-expected output.

### Proposed Fix

When `isShiftClick()`, base the bonus on the per-recipe result amount (e.g. `recipe.getResult()` count scaled by the number of crafted units) instead of the batch total, then clamp the resulting stack to `Material.getMaxStackSize()`.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: shift-crafting 8 items with multiplier 2 yields the expected total, not 2× the batch total
- [x] Unit test: output never exceeds `Material.getMaxStackSize()`
- [x] Unit test: normal single craft behavior is unchanged
