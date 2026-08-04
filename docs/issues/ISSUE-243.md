# ISSUE-243: Bound `max_level` to prevent huge threshold-table allocations

## Context & User Story
- **Goal:** As a server owner, I want a bad `max_level` value to fail fast instead of stalling the main thread with a giant allocation.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `maxLevel` is only validated `>= 1` (`SkillManager.java:154-157`); `LevelThresholds.compute` allocates `long[maxLevel]` + `boolean[maxLevel]` lazily on the first `getLevelForXp` call (`LevelThresholds.java:53-54`), so a config `max_level: 100000000` can stall the main thread with a multi-hundred-MB allocation.

## Implementation Requirements
- [ ] Reject `max_level` above a sane upper bound at parse time (choose a bound that reflects realistic curve sizes; document it in `template-skill.yml`).
- [ ] Add a unit test asserting an oversized `max_level` fails `loadSkills` fast.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/main/resources/template-skill.yml` (document the bound)
  - `src/test/java/io/github/chasehuegel/skilling/skill/SkillDefinitionLevelTest.java`
- **Dependencies:** none.
- **Constraints:** Keep the existing `maxLevel >= 1` check.

## Verification & Definition of Done
- [ ] Oversized `max_level` is rejected at load.
- [ ] Bundled skills (all use sane `max_level`) still parse.
- [ ] `./gradlew build` and `./gradlew test` pass.
