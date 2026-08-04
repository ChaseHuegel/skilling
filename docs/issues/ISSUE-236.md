# ISSUE-236: Validate XP-source trigger keys against the TriggerRegistry at load

## Context & User Story
- **Goal:** As a skill author, I want a typo'd `trigger:` on an `xp_sources` entry rejected at load so I never lose XP to a silently never-firing source.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `SkillManager.parseXpSources` reads the trigger without validating it against `TriggerRegistry` (`SkillManager.java:222-223`), unlike abilities which are validated at `:280-282`. A typo passes load, is never dispatched, and the source silently never fires.

## Implementation Requirements
- [ ] In `SkillManager.parseXpSources`, reject a `trigger` that is not registered in `TriggerRegistry` (same fail-fast message style as the ability path at `:280-282`).
- [ ] Add a unit test asserting a malformed XP-source trigger fails at load and a valid one parses.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/SkillManagerTriggerFieldTest.java`
- **Dependencies:** `TriggerRegistry`.
- **Constraints:** `level_up`, `brew_start`, `brew_potion`, `repair`, etc. are registered keys and must keep parsing; only unknown keys should fail.

## Verification & Definition of Done
- [ ] `xp_sources` with an unknown trigger fails `loadSkills`.
- [ ] All bundled skill YAML still parses.
- [ ] `./gradlew build` and `./gradlew test` pass.
