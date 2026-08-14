# ISSUE-282: `level_up` XP sources can self-trigger an unbounded XP cascade

## Context & User Story
- **Goal:** As a skill author, I want a `level_up` XP source with a positive reward to grant a level-up bonus once per level-up, not to recursively pump XP to max level with fanfare spam. `grantXp` dispatches the `level_up` trigger back through `dispatch(...)`, which re-enters `grantXp`; if the bonus crosses the next threshold, the cascade repeats with no depth guard.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Break the re-entrancy: either track already-dispatched skill ids through the recursion and skip the nested `level_up` dispatch, or evaluate `level_up` sources against the pre-level-up level so they can never chain.
- [ ] Ensure `SkillingLevelUpEvent`, broadcast/fanfare, and unlock feedback fire at most once per actual level-up.
- [ ] Add a unit test: a skill whose `level_up` reward alone would cross several thresholds advances exactly one level per level-up event.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:550-561` (`grantXp` → `dispatch(player, levelUpEvent, "level_up")`). Related: `engine/feedback/LevelUpDispatcher.java`, `engine/event/SkillingLevelUpEvent.java`.
- **Dependencies:** None.
- **Constraints:** Termination must not rely on the `max_level` cap alone. Keep normal XP grant behavior for all other triggers unchanged.

## Verification & Definition of Done
- [ ] New test proves a chaining `level_up` reward does not escalate beyond one level per event.
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: multiple skills with `level_up` sources in one event each level up once.
