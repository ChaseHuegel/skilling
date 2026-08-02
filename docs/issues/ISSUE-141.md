# ISSUE-141: Clear `FeedbackDebouncer` and `BossBarPool` state on player quit

**Status:** Resolved
**Type:** Bug
**Severity:** High (unbounded memory growth per unique player)

---

## Context & User Story

- **Goal:** As a server owner, I want per-player feedback and boss-bar state to be released when a player leaves so memory does not grow without bound on a long-lived server.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Call `feedbackDebouncer.clear(uuid)` in `PlayerListener.onPlayerQuit` (the class Javadoc at `FeedbackDebouncer.java:73-80` explicitly requires this)
- [x] Remove the player's boss bars on quit (`bossBarPool.removeAll(player)` or equivalent) and hide them so no bar is left visible
- [x] Guard against quit racing a level-up/dispatch (only clear, never break in-flight behavior)
- [x] Add a test asserting `tryDebounce` state for a UUID is released after `clear(uuid)`

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/FeedbackDebouncer.java:29,50-62,81`
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/BossBarPool.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java:36-56`
- **Dependencies:** None.
- **Constraints:** All calls happen on the main thread (quit event). Boss-bar removal must also hide the bar (`setVisible(false)`).

### Root Cause

`tryDebounce(UUID, ...)` inserts a per-UUID sub-map via `computeIfAbsent`, and nothing ever removes entries. The class Javadoc instructs calling `clear(uuid)` from `PlayerQuitEvent`, but `onPlayerQuit` never does — nor does it remove boss bars. Every unique player who ever joins permanently leaks a map entry.

### Proposed Fix

In `onPlayerQuit`, clear the debouncer entry for the player and remove/hide their boss bars. Add the missing `clear` test that `FeedbackDebouncerTest.clearRemovesPlayerState` was named for but never actually ran.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new test
- [x] Unit test: after `clear(uuid)`, the debouncer no longer debounces that player (fresh feedback allowed)
- [x] Unit test: quit removes the player's boss bars
- [x] Manual smoke: joining/leaving repeatedly shows stable memory
