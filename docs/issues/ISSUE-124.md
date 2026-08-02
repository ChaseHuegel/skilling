# ISSUE-124: Dispatch `BrewEvent` and `PrepareAnvilEvent` so blocked mechanics become reachable

**Status:** Open
**Type:** Bug
**Severity:** High (two shipped mechanics can never execute)

---

## Context & User Story

- **Goal:** As a skill author, I want `modify_potion_duration` and `repair_discount` mechanics to actually run when their triggers fire.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Add dispatch handlers for `BrewEvent` (inventory brew finish) and `PrepareAnvilEvent` with appropriate trigger keys
- [ ] Align the `brew_potion` trigger with the event actually dispatched (currently `BrewingStartEvent` is dispatched but `BrewPotionTrigger` declares `BrewEvent`)
- [ ] Ensure `ModifyPotionDurationMechanic` (expects `BrewEvent`) and `RepairDiscountMechanic` (expects `PrepareAnvilEvent`) receive the events they require
- [ ] Keep `ModifyBrewTimeMechanic` working on `BrewingStartEvent` (may warrant its own trigger key)
- [ ] Add a unit test verifying each newly-dispatched trigger reaches its mechanic

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:195-204` (dispatch wiring)
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/BrewPotionTrigger.java:17`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyPotionDurationMechanic.java:20-21`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/RepairDiscountMechanic.java:14`
- **Dependencies:** None beyond the trigger registry.
- **Constraints:** Document any new trigger keys in `docs/users/capabilities.md` and update the `brew_potion` semantics.

### Root Cause

No handler dispatches `BrewEvent` or `PrepareAnvilEvent`. `BrewPotionTrigger` is documented/declared on `BrewEvent` while the engine actually dispatches `BrewingStartEvent` under key `brew_potion`. As a result `ModifyPotionDurationMechanic` and `RepairDiscountMechanic` are dead code, and the trigger's documented event class mismatches reality.

### Proposed Fix

Add synchronous dispatch handlers for `BrewEvent` (key `brew_potion` or a new `brew_finish`) and `PrepareAnvilEvent` (key `repair`), update `BrewPotionTrigger` to match, and give `ModifyBrewTimeMechanic` its own start-event trigger if it should remain distinct.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: `BrewEvent` reaches `modify_potion_duration`
- [ ] Unit test: `PrepareAnvilEvent` reaches `repair_discount`
- [ ] Trigger registry keys and `docs/users/capabilities.md` are consistent with actual dispatch
