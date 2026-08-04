# ISSUE-241: Guard LevelUpDispatcher scheduled unlock tasks against a quitting player

## Context & User Story
- **Goal:** As a player, I want a level-up that schedules unlock announcements to never throw on the main thread if I log out before they fire.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `LevelUpDispatcher` (`LevelUpDispatcher.java:120-136`) runs `sendMessage`/`showTitle` on a `Player` via `runTaskLater` up to ~3s after the level-up with no `isOnline()` guard; a disconnected player reference can throw `IllegalStateException` on the main thread. The same codebase already guards this pattern in `SkillEventListener.java:633`.

## Implementation Requirements
- [x] Add an `isOnline()` (and player-still-connected) guard inside each scheduled task lambda in `LevelUpDispatcher` before touching the player, matching the existing pattern.
- [x] Add a unit test (using `BukkitMock` or a fake scheduler) that schedules the announcements, marks the player offline, and asserts the task no-ops without throwing.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/LevelUpDispatcher.java`
  - `src/test/java/io/github/chasehuegel/skilling/feedback/LevelUpDispatcherTest.java`
- **Dependencies:** existing test scheduling infrastructure (`BukkitMock`, `FanfarePendingTest`).
- **Constraints:** None.

## Verification & Definition of Done
- [x] Scheduled level-up/unlock feedback no-ops safely for an offline player.
- [x] `./gradlew build` and `./gradlew test` pass.
