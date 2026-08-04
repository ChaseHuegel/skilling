# ISSUE-246: `/skills reset` leaves a stale XP boss bar visible

## Context & User Story
- **Goal:** As a player, I want a reset skill to immediately stop showing its old XP boss bar, not keep stale progress until the next XP gain or relog.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — the reset handler zeroes XP and invalidates the page cache but never calls `bossBarPool.remove(player, skillId)` (`SkillsCommand.java:459`), so the bar keeps showing the old progress/level.

## Implementation Requirements
- [ ] Remove/hide the skill's boss bar in the reset path (`SkillsCommand.java`, reset handler) so it disappears immediately.
- [ ] Add a unit test asserting the bar is removed on reset.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java`
  - `src/test/java/io/github/chasehuegel/skilling/command/SkillsCommandPageCacheTest.java` (or a new test)
- **Dependencies:** `BossBarPool`.
- **Constraints:** Keep the write-behind cache flush for the reset (offline/online) unchanged.

## Verification & Definition of Done
- [ ] Resetting a skill hides its XP boss bar immediately.
- [ ] `./gradlew build` and `./gradlew test` pass.
