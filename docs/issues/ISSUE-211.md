# ISSUE-211: Quit/reconnect race can evict the live profile of an online player

## Context & User Story
- **Goal:** As a player, I want my skill data to keep working when I rejoin quickly after quitting, without the profile being silently evicted and resetting to level 0 for the session.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Fix the race where the quit handler's async flush completion removes a profile instance that a reconnect has since re-installed.
- [x] The identity-based `unloadProfile(uuid, instance)` guard (`ProfileManager.java:139-141`) currently protects only against a *newer* instance being installed; it does not help when `installHydrated` keeps the *same* dirty instance (`ProfileManager.java:82-89`), which the quit completion then removes by identity.
- [x] Options: capture the profile instance + a session marker at quit and skip removal if the player has rejoined (online again); or remove the profile under the `profiles.compute` in `installHydrated` so the quit removal can never match a re-installed entry; or sequence the flush completion against rejoin. Pick the approach that never leaves an online player without a cached profile.
- [x] Add a regression test covering: quit with a dirty profile -> reconnect before the quit flush completes -> the reconnected session retains its hydrated profile (`ProfileManagerRaceTest` covers the clean-profile case only).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java` (lines 73-92)
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java` (lines 82-89, 139-141)
  - `src/test/java/io/github/chasehuegel/skilling/profile/ProfileManagerRaceTest.java`
- **Dependencies:** none.
- **Constraints:** The race is real because the flush and its completion callback run on pool threads while the pre-login/join sequence can complete on the main thread. The fix must not break the existing protection against evicting genuinely newer profiles.

## Verification & Definition of Done
- [x] Reconnect-while-flush-in-flight leaves the player with a functional cached profile (XP reads persist, fanfares fire).
- [x] New race regression test passes.
- [x] `./gradlew build` and `./gradlew test` pass.
