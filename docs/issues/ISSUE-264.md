# ISSUE-264: onPrepareAnvil MONITOR handler is missing ignoreCancelled

## Context & User Story
- **Goal:** As an engine maintainer, I want the `repair` trigger to fire only on real anvil interactions, not on anvil interactions that another plugin has already cancelled.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — behavioral inconsistency; the only MONITOR handler without `ignoreCancelled`.

## Implementation Requirements
- [ ] Add `ignoreCancelled = true` to the `@EventHandler` on `onPrepareAnvil` (currently the only MONITOR handler that lacks it).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:243-244`
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListenerBrewAnvilTriggerTest.java`
- **Dependencies:** none.
- **Constraints:** Match the pattern of the other MONITOR handlers (block break/place, damage, craft, fish, interact).

## Verification & Definition of Done
- [ ] Cancelled `PrepareAnvilEvent`s no longer trigger `repair`.
- [ ] `./gradlew build` and `./gradlew test` pass.
