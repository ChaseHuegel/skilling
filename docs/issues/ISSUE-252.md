# ISSUE-252: Remove or fix dead `PolynomialEvaluator.xpToNextLevel`

## Context & User Story
- **Goal:** As an engine maintainer, I want no dead code carrying a latent wrong value in the API module.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `PolynomialEvaluator.xpToNextLevel` (`PolynomialEvaluator.java:55-59`) is referenced nowhere in the codebase, returns `totalForNext - currentXp` which is negative whenever the player already has enough XP for the next level (no clamp), computes `totalForCurrent` without using it, and its Javadoc declares a `@param currentXp` the method does not take.

## Implementation Requirements
- [x] Remove the dead method (preferred) or fix its semantics and add a real caller + tests.
- [x] If removed, confirm nothing (including addons) references it.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/PolynomialEvaluator.java`
  - `src/test/java/io/github/chasehuegel/skilling/evaluator/PolynomialEvaluatorTest.java`
- **Dependencies:** none.
- **Constraints:** If any addon-facing surface exposes it, check `docs/users/api-integration.md`; it is `public` in the engine module (not `skilling-api`), so removal is safe.

## Verification & Definition of Done
- [x] Method removed (or fixed with tests and a caller).
- [x] `./gradlew build` and `./gradlew test` pass.
