# ISSUE-114: Fix 3× loot duplication in `YieldMultiplierMechanic`

**Status:** Open
**Type:** Bug
**Severity:** Critical (item duplication exploit)

---

## Context & User Story

- **Goal:** As a player, I want the yield multiplier ability to double a block's drops once, not produce the vanilla drops plus a doubled clone (triple loot).
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Cancel the vanilla drops (`breakEvent.setDropItems(false)`) when the mechanic runs and spawns its own doubled drops
- [ ] Keep the doubling math: natural drops × 2 dropped once at the block location
- [ ] Preserve the existing `yield_chance` roll semantics (percentage chance)
- [ ] Add a unit test asserting vanilla drops are suppressed and exactly the doubled amount is dropped

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/YieldMultiplierMechanic.java:22-41`
- **Dependencies:** Compare `AutoSmeltMechanic.java:45`, which correctly calls `be.setDropItems(false)` before spawning its own drops.
- **Constraints:** Do not change the behavior of blocks that legitimately drop nothing. Only suppress vanilla drops on the triggered event.

### Root Cause

The mechanic simulates `block.getDrops(hand)`, doubles each stack, and spawns the doubled copies with `dropItemNaturally` — but never cancels the original drop pipeline. The vanilla `BlockBreakEvent` still drops the original items, so a single break yields original (1×) + clone (2×) = 3× loot.

### Proposed Fix

Inside the `execute` path that spawns the doubled drops, call `breakEvent.setDropItems(false)` first so the only items that appear are the mechanic's. The class Javadoc (`YieldMultiplierMechanic.java:12-18`) should be updated to state that vanilla drops are replaced by the doubled set.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: `setDropItems(false)` is invoked and only doubled drops are spawned
- [ ] Unit test: `yield_chance` of 0 or <= 0 leaves vanilla drops untouched
