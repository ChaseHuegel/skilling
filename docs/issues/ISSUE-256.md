# ISSUE-256: Unify the duplicated ConstantEvaluator / ConstantValueEvaluator handling

## Context & User Story
- **Goal:** As an engine maintainer, I want a single, unambiguous constant-evaluator contract instead of two classes both claiming the YAML key `constant` and requiring bespoke special-casing at every use site.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low (maintainability) — the API `ConstantEvaluator` (double) and the engine `ConstantValueEvaluator` (string) both document YAML key `constant`, and code special-cases them at four+ sites (`SkillEventListener.evaluateParams:878`, `SkillManager.constantValueOf:541-545`, `LoreResolver:49`, `SkillMenuBuilder:240-244`), which is easy to mishandle in future code.

## Implementation Requirements
- [x] Collapse to one evaluator (e.g. have `ConstantEvaluator` carry a `double` and a string variant, or make the constant evaluator polymorphic) and remove the special-casing at every use site so constant parameters flow through the generic `evaluate(...)` path.
- [x] Ensure `ConstantValueEvaluator`'s string value (e.g. namespaced effect keys) survives the round-trip through `SkillManager.constantValueOf` for load-time validation and the web serializer.
- [x] Add tests covering constant string-valued parameters (effect keys) end-to-end.

## Technical Specifications & Context
- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/ConstantEvaluator.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/ConstantValueEvaluator.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/LoreResolver.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillMenuBuilder.java`
- **Dependencies:** none.
- **Constraints:** Greenfield — no compat obligation; the web serializer round-trip tests must keep passing.

## Verification & Definition of Done
- [x] One constant-evaluator contract; no `instanceof ConstantValueEvaluator` special-cases remain.
- [x] String-valued constant parameters still round-trip through load validation and the web GUI.
- [x] `./gradlew build` and `./gradlew test` pass.
