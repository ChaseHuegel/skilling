# ISSUE-249: LinearEvaluator does not validate finite base/step/min/max

## Context & User Story
- **Goal:** As a skill author, I want a `.nan`/`.inf` evaluator value rejected at load like the polynomial evaluator does, not surfacing as NaN behavior on the event path.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `LinearEvaluator`'s constructor only checks `min > max` (`LinearEvaluator.java:35-44`); NaN comparisons are false, so SnakeYAML `.nan`/`.inf` values pass load and then reach `Math.clamp` on the event path, producing NaN results or throwing. `PolynomialEvaluator` already validates finiteness.

## Implementation Requirements
- [x] Validate that `base`, `step`, `min`, `max` are finite (where provided) in the `LinearEvaluator` constructor, mirroring `PolynomialEvaluator`'s finiteness checks.
- [x] Add a unit test asserting NaN/Infinity inputs fail construction.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/LinearEvaluator.java`
  - `src/test/java/io/github/chasehuegel/skilling/evaluator/LinearEvaluatorTest.java`
- **Dependencies:** `PolynomialEvaluator` as the reference pattern.
- **Constraints:** `min`/`max` may legitimately be `-Infinity`/`+Infinity` when unspecified (they default that way) — only reject explicit NaN/Infinity *values*, or reject non-finite non-sentinel inputs.

## Verification & Definition of Done
- [x] NaN/Infinity `base`/`step`/`min`/`max` fail at construction.
- [x] Default sentinel `±Infinity` bounds still work.
- [x] `./gradlew build` and `./gradlew test` pass.
