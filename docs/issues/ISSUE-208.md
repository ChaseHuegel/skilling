# ISSUE-208: Gate the `fishing` trigger on the caught-fish event state

## Context & User Story
- **Goal:** As a server admin, I want the `fishing` trigger to fire once per caught fish so the bundled Fishing skill does not over-grant XP and re-fire abilities on every fishing-event state transition.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] `SkillEventListener.onFish` must only dispatch the `fishing` trigger when `event.getState() == PlayerFishEvent.State.CAUGHT_FISH` (or otherwise gate on the state that represents a completed catch). Casts (`FISHING`), bites (`CAUGHT_ENTITY`), reels, and `FAILED_ATTEMPT` must not grant XP or fire abilities.
- [ ] If state gating is the chosen fix, confirm the bundled `fishing.yml` XP source and abilities need no per-source state filter (the single gate covers them all).
- [ ] Reconcile `fishing.yml` comment ("Treasure-catch source deferred (no fish-state filter yet)") with the new behavior; the deferred treasure source remains deferred only if the state-gate change does not enable it.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (lines 279-282)
  - `src/main/resources/skills/fishing.yml` (XP source at lines 15-18)
  - `docs/users/capabilities.md` (trigger semantics, if the state gate changes documented behavior)
- **Dependencies:** none.
- **Constraints:** `PlayerFishEvent` fires once per state transition (FISHING, CAUGHT_ENTITY, REEL_IN, CAUGHT_FISH, IN_GROUND, FAILED_ATTEMPT). Today the bundled Fishing skill grants 138.9 XP per dispatch with no state filter, so a single cast-and-catch grants the reward 3-4x and abilities (angler, lucky_catch, etc.) re-fire per state. Do not change the reward values.

## Verification & Definition of Done
- [ ] A single cast-and-catch grants exactly one XP reward and fires fishing abilities exactly once; a cast with no catch grants nothing.
- [ ] `./gradlew build` passes.
- [ ] `./gradlew test` passes.
