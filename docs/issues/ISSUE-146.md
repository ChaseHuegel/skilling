# ISSUE-146: Invalidate the page-inventory cache on every XP/level mutation path

**Status:** Resolved
**Type:** Bug
**Severity:** Medium (stale GUI after admin level commands)

---

## Context & User Story

- **Goal:** As an admin, I want the skill GUI to reflect the levels I set with `/skills setlevel`, `/skills addxp`, or `/skills reset` immediately, not show stale cached pages.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Call `profile.invalidatePageCache()` in every XP/level mutation path: `SkillsCommand.setXp`/`addXp`/`reset` (currently only the event path in `SkillEventListener.java:402` does this)
- [x] Cover the reset-to-zero path (`SkillsCommand.java:388-393`)
- [x] Add a unit test asserting the page cache is invalidated after an admin level change

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java:273,334,388-393`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:402` (correct pattern to mirror)
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/PlayerProfile.java:209-219`
- **Dependencies:** `SkillMenuBuilder.buildPaginatedOverview` (cache consumer).
- **Constraints:** Invalidation must be safe when the player is offline (no-op).

### Root Cause

`setXp`/`addXp`/`setXp(id,0)` are called directly by admin commands; only the event path invalidates the cached page inventories. After `/skills setlevel`, the player's next GUI open returns the stale cached inventory showing old levels. `reset` also leaves the cache stale.

### Proposed Fix

Invalidate the profile's page cache in every admin mutation path, guarding for offline/no-profile cases.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new regression test
- [x] Unit test: `/skills setlevel` invalidates the cached pages
- [x] Unit test: `/skills reset` invalidates the cached pages
- [x] Manual smoke: setlevel then open GUI shows the new level (by design: cache invalidation forces a rebuild on next open)
