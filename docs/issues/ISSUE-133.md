# ISSUE-133: Replace silent parsing failures with fail-fast errors in `SkillManager` and `CustomTagLoader`

**Status:** Resolved
**Type:** Bug
**Severity:** Medium (bad configs silently disable features)

---

## Context & User Story

- **Goal:** As a skill author, I want a malformed XP source, cooldown, mechanic, or tag definition to be rejected at load with a clear message instead of silently granting 0 XP or disabling a feature.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Throw `IllegalArgumentException` at parse time for: missing `reward` in an XP source, string-valued numeric fields (e.g. `cooldown: "5"`), unknown mechanic types, non-map entries in `xp_sources`/abilities, and string `unlock_level` values
- [x] Make `CustomTagLoader.load` fail fast on malformed tag definitions instead of catching everything and leaving tags empty
- [x] Ensure the failure surface surfaces during `/skills reload` (see ISSUE-152 for reload error handling)
- [x] Add unit tests for each silent-failure path asserting a clear exception

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:159,177-178,200,262-267,291-292,414-418`
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/CustomTagLoader.java:57-59`
- **Dependencies:** Fail-fast convention (`src/AGENTS.md` §9).
- **Constraints:** Do not silently coerce wrong-typed values to `0.0`. Wrong exception type (`ClassCastException` instead of `IllegalArgumentException`) is also a defect.

### Root Cause

Missing `reward` → empty map → `ConstantEvaluator(0.0)` (source silently grants 0 XP). `cooldown: "5"` (string) → `ConstantEvaluator(0.0)` (cooldown silently disabled). Wrong mechanic type → `List.of()` (no mechanics). Non-map entries are silently `continue`d. `unlock_level: "5"` → raw `(Number)` cast → `ClassCastException`. `CustomTagLoader.load` catches everything and logs a warning, leaving tags empty.

### Proposed Fix

Add strict type/required-key validation in each parse path and throw descriptive `IllegalArgumentException`s. Let `CustomTagLoader` propagate malformed-definition errors so reload surfaces them.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: XP source without `reward` throws at parse
- [x] Unit test: string-valued numeric fields throw a descriptive `IllegalArgumentException`
- [x] Unit test: unknown mechanic type throws instead of silently producing no mechanics
- [x] Unit test: malformed `tags.yml` fails load with a clear message
