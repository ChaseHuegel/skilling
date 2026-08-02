# ISSUE-169: Clean up test-quality issues (misnamed, no-op, duplicated, and guard-only tests)

**Status:** Open
**Type:** Improvement
**Severity:** Low (tests that mislead or assert nothing)

---

## Context & User Story

- **Goal:** As a developer, I want the test suite to accurately describe and actually verify the behavior it claims to cover.
- **Agent Role:** You are an expert QA engineer executing this task.

## Implementation Requirements

- [x] Fix `FeedbackDebouncerTest.clearRemovesPlayerState` (`FeedbackDebouncerTest.java:43-54`) — it never calls `clear()`; implement the clear-path assertion (and see ISSUE-141)
- [x] De-duplicate the byte-identical bulk tests in `SkillEventListenerBulkScalarTest.java:63-79` into a parameterized test
- [x] Remove `RequirementEngineTest.java:34-50` duplicate assertions already covered by `RequirementResultTest`
- [x] Fix guard-only mechanic tests so core behavior is verified (e.g. `ModifyDamageMechanicTest` asserts `execute` returns true but never verifies the damage was modified; add `verify(event).setDamage(...)` or equivalent)
- [x] Remove or convert the assert-nothing `FanfareDispatcherTest` smoke tests
- [x] Remove the ignored `player` parameter in `BukkitMock.mockBlockBreakEvent(Player)` (`BukkitMock.java:54-56`)
- [x] Add isolation for tests relying on the static `XpBonusMechanic` multiplier map (see ISSUE-120) so unique-UUID dependence is not the only guard
- [x] Fix `SkillYamlValidationTest.skillFiles()` (`SkillYamlValidationTest.java:151-156`) which resolves resources relative to CWD — resolve via classpath instead so it passes from any working directory
- [x] `./gradlew build && ./gradlew test` must pass after cleanup

## Technical Specifications & Context

- **Target Files:**
  - `src/test/java/io/github/chasehuegel/skilling/feedback/FeedbackDebouncerTest.java:43-54`
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListenerBulkScalarTest.java:63-79`
  - `src/test/java/io/github/chasehuegel/skilling/requirements/RequirementEngineTest.java:34-50`
  - `src/test/java/io/github/chasehuegel/skilling/mechanic/{ModifyDamageMechanicTest,LifestealMechanicTest,ExecuteMechanicTest,YieldMultiplierMechanicTest,ChainBreakMechanicTest,DodgeMechanicTest}.java`
  - `src/test/java/io/github/chasehuegel/skilling/feedback/FanfareDispatcherTest.java:39-62`
  - `src/test/java/io/github/chasehuegel/skilling/BukkitMock.java:54-56`
  - `src/test/java/io/github/chasehuegel/skilling/engine/SkillYamlValidationTest.java:151-156`
- **Dependencies:** ISSUE-120 (XpBonusMechanic static state) and ISSUE-141 (FeedbackDebouncer clear).
- **Constraints:** Tests must remain deterministic. Do not weaken assertions.

### Root Cause

Several tests mislead: `FeedbackDebouncerTest.clearRemovesPlayerState` never calls `clear` (with a stale comment claiming the method doesn't exist — it does); three bulk tests are byte-identical; `RequirementEngineTest` duplicates `RequirementResultTest`; many mechanic tests only exercise the false/no-op guards; `FanfareDispatcherTest` asserts nothing; `BukkitMock.mockBlockBreakEvent` ignores its `player` parameter; and `SkillYamlValidationTest` depends on the process CWD.

### Proposed Fix

Correct each test per the requirements above: parameterize, remove duplicates, assert real behavior (verify mock interactions/effects), delete assert-nothing tests or make them assert, drop the dead parameter, and resolve test resources via the classpath.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass after cleanup
- [x] No test is misnamed or asserts nothing
- [x] No byte-identical duplicated test bodies remain
- [x] `SkillYamlValidationTest` passes from a non-project CWD
