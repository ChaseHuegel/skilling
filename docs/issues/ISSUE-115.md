# ISSUE-115: Fix 3× catch duplication in `FishingYieldMechanic`

**Status:** Open
**Type:** Bug
**Severity:** Critical (item duplication exploit)

---

## Context & User Story

- **Goal:** As a player, I want the fishing yield multiplier to double my caught items once, not let me keep the original catch and also collect a doubled clone (triple catch).
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Ensure the mechanic augments/replaces the caught item rather than adding a full extra doubled stack on top of the original
- [ ] Handle the caught item entity correctly (apply the multiplier to the caught `Item`/inventory result so total is exactly 2×, not 3×)
- [ ] Keep the existing percentage/multiplier parameters
- [ ] Add a unit test asserting the total caught amount equals original × multiplier (no leftover original)

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/FishingYieldMechanic.java:28-35`
- **Dependencies:** Compare `FishingLootMechanic.java`, which correctly drops only the *extra* amount on top of the existing catch.
- **Constraints:** Do not spawn a second item stack that coexists with the original caught item.

### Root Cause

When a fish is caught, the original caught `Item` entity remains. The mechanic drops an additional stack sized `original × 2` at the same location. The player collects the original (1×) plus the clone (2×) = 3× total.

### Proposed Fix

Apply the multiplier to the caught stack in place (or drop only the difference, original × (multiplier − 1)) so the player's total equals original × multiplier. Update the class Javadoc to state the net multiplier applied to the catch.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: a catch of amount N with multiplier 2 results in exactly 2N total (no extra clone)
- [ ] Unit test: multiplier 1 leaves the catch unchanged
