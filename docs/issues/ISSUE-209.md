# ISSUE-209: Fail-fast on scalar YAML values where an evaluator block is required

## Context & User Story
- **Goal:** As a skill author, I want a mis-typed scalar (e.g. `reward: 50` instead of `reward: { constant: 50 }`) rejected at load with a clear error, not silently parsed as `ConstantEvaluator(0.0)` which zeroes every reward or mechanic parameter.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `parseInlineEvaluator` must not silently return `ConstantEvaluator(0.0)` when handed an empty map. Instead, distinguish "no value supplied" (callers that legitimately default) from "scalar value supplied where a map was expected" (reject with `IllegalArgumentException`).
- [x] `parseXpSources` reward parsing rejects a scalar `reward:` (and any non-map non-evaluator value) with a descriptive error naming the skill file and trigger.
- [x] Mechanic `parameters:` entries reject scalar values (e.g. `parameters: { multiplier: 2 }`) with a descriptive error instead of evaluating to 0.0.
- [x] Confirm `castMap` (SkillManager.java:606-614) is not the silent-failure point; either reject non-map input at the call sites above or make the fallback loud.
- [x] Add load-time validation tests covering: scalar `reward`, scalar mechanic parameter, and the valid `constant:`/`linear:`/`milestones:` block forms still parse unchanged.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java` (lines 209-246, 434-483, 529-531, 606-614)
  - `src/test/java/io/github/chasehuegel/skilling/engine/SkillManagerTest.java` or a new load-validation test
- **Dependencies:** none.
- **Constraints:** Greenfield — rejecting previously-"accepted" scalar input is allowed. Bundled skill YAML all uses the block form, so no bundled config should break. The `parseCooldown` scalar path is intentionally supported and must keep working.

## Verification & Definition of Done
- [x] A skill with `reward: 50` fails to load with a descriptive `IllegalArgumentException`; the same for a scalar mechanic parameter.
- [x] Bundled skills still load (`./gradlew build`).
- [x] New unit tests for the scalar-rejection paths pass (`./gradlew test`).
