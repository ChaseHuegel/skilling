# ISSUE-131: Add fail-fast numeric validation to parameter evaluators

**Status:** Resolved
**Type:** Bug
**Severity:** Medium (bad curves produce NaN/`Long.MAX_VALUE` XP at runtime)

---

## Context & User Story

- **Goal:** As a server owner, I want a malformed XP curve or evaluator config to be rejected at load with a clear error, never to silently grant instant max level or zero XP during gameplay.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Validate evaluator numeric params at parse time: reject NaN/Infinity-producing inputs (e.g. negative base/exponent in `PolynomialEvaluator`), and validate `min <= max` for clamped evaluators (`LinearEvaluator`)
- [x] Guard `computeXpGain` (`SkillEventListener.java:655-660`) and `getLevelForXp` (`SkillDefinition.java:259-265`) against NaN/Infinity (`Math.round(+Infinity)` = `Long.MAX_VALUE`; `Math.round(NaN)` = 0)
- [x] Add unit tests covering: negative base/exponent rejected at load, `min > max` rejected, NaN/Infinity XP inputs produce no level change and no crash

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/PolynomialEvaluator.java:21-34`
  - `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/LinearEvaluator.java:44-45`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:655-660`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/SkillDefinition.java:259-265`
- **Dependencies:** `SkillManager.parseProgression` / `parseInlineEvaluator` (where validation should live).
- **Constraints:** Follow the fail-fast convention (`src/AGENTS.md` §9): throw `IllegalArgumentException` during parsing.

### Root Cause

No numeric validation exists. A negative `base_xp` or `exponent` makes `Math.pow` produce negative/Infinity values; `Math.round(+Infinity)` = `Long.MAX_VALUE` (instant max level) and `Math.round(NaN)` = 0 (silent zero XP). `LinearEvaluator` calls `Math.clamp` with no load-time `min <= max` check, throwing at runtime during gameplay.

### Proposed Fix

Validate at parse: reject non-finite or out-of-range parameters with a descriptive `IllegalArgumentException`. In the runtime paths, treat NaN/Infinity results as 0 XP (or skip) and never compute `Long.MAX_VALUE` levels.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: malformed polynomial config throws at load with a clear message
- [x] Unit test: `min > max` linear config throws at load
- [x] Unit test: NaN/Infinity XP input does not crash or max out a level
