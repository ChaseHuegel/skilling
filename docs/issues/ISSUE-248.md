# ISSUE-248: ModifyFurnaceOutputMechanic can create an oversized ItemStack on large multipliers

## Context & User Story
- **Goal:** As a player, I want a high `core:modify_furnace_output` multiplier to never produce an ItemStack whose amount exceeds the stack cap (which can clamp or merge unexpectedly, losing bonus items).
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `new ItemStack(extractEvent.getItemType(), bonus)` (`ModifyFurnaceOutputMechanic.java:31`) does not split the bonus into capped stacks, unlike `YieldMultiplierMechanic` (`YieldMultiplierMechanic.java:39-44`).

## Implementation Requirements
- [x] Cap/split the produced stack to `maxStackSize` (give the remainder as a separate stack to the inventory or drop it), mirroring the split loop in `YieldMultiplierMechanic`.
- [x] Add a unit test with a multiplier large enough to exceed the stack cap.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyFurnaceOutputMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/mechanic/ModifyFurnaceOutputMechanicTest.java`
- **Dependencies:** `YieldMultiplierMechanic` as the reference split pattern.
- **Constraints:** None.

## Verification & Definition of Done
- [x] Bonus output never exceeds the item stack cap.
- [x] `./gradlew build` and `./gradlew test` pass.
