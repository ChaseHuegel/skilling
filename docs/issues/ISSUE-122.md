# ISSUE-122: Fix inverted probability in `ModifyTameChanceMechanic`

**Status:** Resolved
**Type:** Bug
**Severity:** High (higher multiplier makes taming worse)

---

## Context & User Story

- **Goal:** As a player, I want a higher tame-chance multiplier to make taming more likely, not less.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Correct the probability math so the multiplier increases the chance of a successful tame
- [x] Add the required class-level Javadoc (currently missing) documenting the YAML key and parameter semantics (`src/AGENTS.md` §7)
- [x] Add unit tests: multiplier > 1 increases tame success frequency; multiplier = 1 leaves vanilla behavior

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyTameChanceMechanic.java:15-20`
- **Dependencies:** `EntityTameEvent` via the `tame_entity` trigger.
- **Constraints:** Fail-fast on non-positive multipliers. Keep the mechanic generic (no hardcoded entities).

### Root Cause

`EntityTameEvent` fires only **after** a successful tame roll. The current logic `if (nextDouble() > 1.0/multiplier) tameEvent.setCancelled(true)` cancels a successful tame with probability `1 - 1/multiplier`, which **increases** as the multiplier grows (multiplier 2 → 50% chance to undo a success).

### Proposed Fix

Re-roll the tame outcome: on a successful tame, cancel it with probability `1 - multiplier` (clamped) when `multiplier < 1`; for `multiplier > 1` keep the success (and optionally attempt a re-roll for entities that allow multiple attempts). The exact semantics should be documented in the new Javadoc.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new unit tests
- [x] Unit test: multiplier 2 no longer cancels roughly 50% of successful tames (stochastic test with a seeded deterministic check)
- [x] Unit test: multiplier 1 does not alter tame events
- [x] Class Javadoc documents the parameter and behavior
