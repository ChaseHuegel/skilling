# ISSUE-253: Harden `SkillManager.parseFeedback` boolean casts

## Context & User Story
- **Goal:** As a skill author, I want a malformed `feedback.notify` value rejected cleanly at load instead of a raw `ClassCastException` (or accepted with a tolerant parse).
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `parseFeedback` casts `(boolean) notify.getOrDefault("action_bar", false)` (`SkillManager.java:552-553`); a non-boolean YAML scalar in that slot raises an uncaught `ClassCastException` during load rather than a descriptive `IllegalArgumentException`.

## Implementation Requirements
- [ ] Replace the raw `(boolean)` casts in `parseFeedback` with a tolerant/fail-fast parse (e.g. `MechanicParamValidators`-style boolean helper that throws a descriptive `IllegalArgumentException` on a non-boolean).
- [ ] Add a unit test feeding a malformed `notify` value.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/SkillYamlValidationTest.java`
- **Dependencies:** none.
- **Constraints:** Keep accepting real YAML booleans.

## Verification & Definition of Done
- [ ] Malformed `notify` values fail load with a clear message.
- [ ] Valid feedback still parses.
- [ ] `./gradlew build` and `./gradlew test` pass.
