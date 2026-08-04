# ISSUE-255: Make FeedbackDebouncer check-then-act atomic

## Context & User Story
- **Goal:** As an engine maintainer, I want the debouncer to never emit a duplicate within the interval, even under concurrent callers.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `FeedbackDebouncer.tryDebounce` uses a non-atomic read-then-write (`abilities.get(abilityId)` → `abilities.put(...)`, `FeedbackDebouncer.java:62-70`), so two concurrent callers (the class is exposed via `SkillingAPI.getFeedbackDebouncer()`) can both observe a stale value and both return `true`.

## Implementation Requirements
- [ ] Replace the check-then-act with a single atomic `abilities.compute(...)` so only one caller per interval returns `true`.
- [ ] Add a unit test that asserts a single emission across concurrent calls (reuse the `FeedbackDebouncerTest` harness).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/FeedbackDebouncer.java`
  - `src/test/java/io/github/chasehuegel/skilling/feedback/FeedbackDebouncerTest.java`
- **Dependencies:** none.
- **Constraints:** Keep the per-player, per-ability semantics and the `clear(player)` API.

## Verification & Definition of Done
- [ ] Concurrent callers emit at most one message per interval.
- [ ] `./gradlew build` and `./gradlew test` pass.
