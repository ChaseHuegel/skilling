# ISSUE-136: Optimize `getLevelForXp` and remove per-ability/per-event recomputation

**Status:** Open
**Type:** Improvement
**Severity:** Medium (O(maxLevel) `Math.pow` per ability per event)

---

## Context & User Story

- **Goal:** As a server owner, I want level computation to be cheap so a single event does not trigger thousands of `Math.pow` evaluations across 32 skills × abilities.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Cache per-skill `getLevelForXp` results per event (compute once per skill per dispatch, not once per XP source / per ability)
- [ ] Optimize `getLevelForXp` itself: replace the O(maxLevel) linear scan (with `Math.pow` per step) with a closed-form/binary-search or precomputed threshold table that stays correct after XP curve changes
- [ ] Keep behavior identical for valid XP values (same level thresholds)
- [ ] Add a correctness test proving cached/computed levels match the previous linear scan for a range of XP values

## Technical Specifications & Context

- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/SkillDefinition.java:259-265`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:384,393,426`
- **Dependencies:** None beyond `SkillDefinition` evaluators.
- **Constraints:** XP curves are data-driven; the optimization must respect arbitrary evaluator output per level.

### Root Cause

For every XP source and every ability on every event, `getLevelForXp` loops 1..maxLevel, evaluating the curve (`Math.pow`) per step. With 32 bundled skills, ~100 levels, and 3 calls per source, a single event can trigger thousands of evaluations. `fireAbilities` also recomputes `getLevelForXp` per ability (line 426) when `grantXp` already computed old/new levels.

### Proposed Fix

Compute the level once per skill per event and share it across XP sources and abilities. For the level function, if the curve is a known closed form, invert it; otherwise precompute a level→threshold table once per skill load (invalidated on reload) and binary-search it.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new correctness test
- [ ] Correctness test: optimized `getLevelForXp` matches the linear scan for XP = {0, mid, boundary, max}
- [ ] Profile/benchmark shows level computation no longer dominates event dispatch
