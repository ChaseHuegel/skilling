# ISSUE-213: Isolate mechanic execution failures so a throwing mechanic cannot skip ability cost/consume

## Context & User Story
- **Goal:** As a server admin, I want a single failing mechanic to never abort the whole dispatch — which today lets a player re-trigger the ability instantly with no cooldown or item cost.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `SkillEventListener.fireAbilities` must not let a thrown `mechanic.execute(...)` propagate out of the ability loop (`SkillEventListener.java:564`).
- [x] Per-mechanic failure handling: catch the exception, log it (SEVERE/WARNING) with the skill and mechanic type, mark that mechanic as not executed, and continue to the next mechanic and next ability.
- [x] The Check -> Execute -> Consume lifecycle must still hold: `requirementEngine.consume` (line 581) must not be skipped because a later/other mechanic threw, and a mechanic that threw must not count as an executed activation that spends the ability cost if it produced no effect.
- [x] Add a regression test with a mechanic stub that throws, asserting: the dispatch completes, the remaining abilities run, and consume/cooldown behavior matches the failure semantics chosen.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (lines 549-581)
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListenerFireAbilitiesTest.java` (or new test)
- **Dependencies:** `SkillMechanic` interface and `MechanicRegistry.create`.
- **Constraints:** Mechanics are constructed per dispatch via reflection (`mechanicRegistry.create`, line 551); a construction failure should also be isolated. Keep the existing behavior where a mechanic returning `false` (no-op, wrong event type) does not consume the cost.

## Verification & Definition of Done
- [x] A throwing mechanic no longer aborts the event dispatch or skips other abilities' execution.
- [x] Cost/cooldown consumption is correct after a throwing mechanic.
- [x] New regression test passes; `./gradlew build` and `./gradlew test` pass.
