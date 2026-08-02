# ISSUE-168: Add unit-test coverage for critical untested code paths

**Status:** Open
**Type:** Improvement
**Severity:** Medium (core paths and the entire published API module are untested)

---

## Context & User Story

- **Goal:** As a developer, I want the highest-risk code paths covered by tests so regressions are caught before release.
- **Agent Role:** You are an expert QA/backend engineer executing this task.

## Implementation Requirements

- [ ] Add tests for `SkillDefinition.getLevelForXp(long)` (currently zero coverage; the per-event hot path)
- [ ] Cover `RequirementEngine` exhaustion check, exhaustion consume, the `possession` action, tag-based `#` matching in `hasItem`/`hasItems`/`removeItems`, and the cost-failure path
- [ ] Cover `TagResolver` single-material resolution and the vanilla `#minecraft:` path (the `mockStatic(Bukkit)` pattern already exists in `BossBarPoolTest`)
- [ ] Cover `CustomTagLoader` parsing a real `tags.yml` (materials, vanilla cross-refs, circular refs); fix the existing `parsesTagKeys` test which loads a nonexistent file
- [ ] Add `skilling-api/src/test` covering `Registries`, `EvaluatorRegistry`, `MechanicRegistry`, `TriggerRegistry`, and `PlayerProfileView` (the API module currently has zero tests)
- [ ] Add tests for the new registry behavior from ISSUE-137/138/140 (custom evaluator registration, view contract, defensive copies)
- [ ] `./gradlew build && ./gradlew test` must pass with the new coverage

## Technical Specifications & Context

- **Target Files:**
  - `src/test/` mirroring `src/main` (existing pattern)
  - `skilling-api/src/test` (new)
  - Specific targets: `SkillDefinition.java:259-265`, `RequirementEngine.java:76-93,97-105,138-141,164-209`, `TagResolver.java:42-88`, `CustomTagLoader.java`
- **Dependencies:** ISSUE-137/138/140 change the registry surface; coverage should target the post-change behavior.
- **Constraints:** Follow `src/AGENTS.md` Verification ("What to Test"). Deterministic tests only (no sleeps/timeouts).

### Root Cause

`getLevelForXp`, the requirement exhaustion/possession/tag paths, vanilla-tag resolution, and real `tags.yml` parsing are untested. The published API module has no tests at all. The existing `CustomTagLoaderTest.parsesTagKeys` loads a nonexistent file and asserts emptiness — it parses nothing.

### Proposed Fix

Add focused unit tests per the requirements above, using the existing `BukkitMock` and `mockStatic(Bukkit)` patterns, and stand up a real fixture `tags.yml` for `CustomTagLoaderTest`.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass with all new tests
- [ ] Coverage added for each listed path (verify via test list, not a coverage tool)
- [ ] `:skilling-api:build` passes with the new API-module tests
- [ ] No flaky (sleep/time-based) tests introduced
