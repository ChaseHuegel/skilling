# ISSUE-137: Wire `EvaluatorRegistry` into parsing so custom evaluators actually work

**Status:** Resolved
**Type:** Bug
**Severity:** Critical (published addon extension point is a silent no-op)

---

## Context & User Story

- **Goal:** As an addon developer, I want a custom evaluator I register through the API to be usable as a YAML evaluator type, exactly as the API documentation promises.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Make `SkillManager.parseProgression` and `parseInlineEvaluator` consult `EvaluatorRegistry` for the evaluator type instead of hardcoding `polynomial`/`linear`/`constant`/`milestones`
- [x] Validate the registered evaluator contract (must be a `ParameterEvaluator` with the expected evaluate signature) at registration and/or parse time with clear errors
- [x] Keep built-in evaluators registered exactly as today so all bundled YAML parses unchanged
- [x] Add unit tests covering: a registered custom evaluator type parses and evaluates; an unregistered type still fails fast with `IllegalArgumentException`

## Technical Specifications & Context

- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/EvaluatorRegistry.java:25-40`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/evaluator/ParameterEvaluator.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:136-152,353-419` (hardcoded construction)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:255-258` (builtin registration)
- **Dependencies:** `docs/users/api-integration.md:273-275` documents custom evaluator registration.
- **Constraints:** `EvaluatorRegistry.get()`/`contains()` are currently never called in `src/`; this issue makes them load-bearing. Builtin behavior must not regress.

### Root Cause

The engine's parser constructs evaluators via `new` for only the four hardcoded types and never reads the registry. An addon registering `"logistic"` stores it in a map that only the startup log count and bStats read; the YAML key `logistic` falls through to `IllegalArgumentException("Unknown evaluator type...")`. This is a broken promise of the published API.

### Proposed Fix

Route evaluator construction through `EvaluatorRegistry.get(type)` with a fallback to the built-ins, so registered evaluators are invoked. Fail fast with a clear message for unregistered types.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: registering `logistic` then parsing `evaluator_type: logistic` succeeds and evaluates correctly
- [x] Unit test: unregistered type still throws `IllegalArgumentException` at parse
- [x] All bundled skill YAML parses unchanged (existing `SkillYamlValidationTest` passes)
- [x] `docs/users/api-integration.md` example remains accurate (now actually functional)
