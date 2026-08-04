# ISSUE-238: Fix the `linear` progression curve's level-1 threshold offset

## Context & User Story
- **Goal:** As a skill author, I want `curve: linear` with a given `base_xp` to require exactly `base_xp` at level 1, matching the `polynomial` curve and the documented "base XP for level 1".
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — switching a skill's curve from `polynomial` to `linear` with identical `base_xp` silently changes the whole curve offset: level 1 requires `1.1 × base_xp` instead of `base_xp`.

## Implementation Requirements
- [x] `SkillManager.parseProgression` registers `new LinearEvaluator(baseXp, baseXp * 0.1, 0, Double.MAX_VALUE)` (`SkillManager.java:201`) and `LevelThresholds.compute` evaluates thresholds with `unlockLevel = 0` (`LevelThresholds.java:58`). `LinearEvaluator` computes `base + step * (currentLevel - unlockLevel)`, so level 1 yields `baseXp * 1.1`. Make the linear curve's level-1 threshold exactly `baseXp` (e.g. register `step = baseXp * 0.1` evaluated from level 1, or pass `unlockLevel = 1` for progression thresholds) — pick one contract, document it, and keep ability parameter evaluation (which passes the real `unlockLevel`) correct.
- [x] Add a unit test asserting `linear` and `polynomial` with the same `base_xp` both require `base_xp` at level 1.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/LevelThresholds.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/LinearEvaluator.java` (if the contract changes here)
  - `src/test/java/io/github/chasehuegel/skilling/evaluator/LinearEvaluatorTest.java`
- **Dependencies:** none.
- **Constraints:** Bundled skills that use `curve: linear` must not regress; verify against the bundled YAML and `template-skill.yml`.

## Verification & Definition of Done
- [x] `linear` curve level-1 threshold equals `base_xp`.
- [x] Ability parameter evaluation with non-zero `unlock_level` is unaffected.
- [x] `./gradlew build` and `./gradlew test` pass.
