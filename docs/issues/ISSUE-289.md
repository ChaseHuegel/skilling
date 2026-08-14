# ISSUE-289: Failed preference load silently overwrites the player's real preferences on the next flush

## Context & User Story
- **Goal:** As a player, I want my logging preferences to survive a transient database blip at login. `loadPreferences` swallows any exception, leaving the profile at `PlayerPreferences.DEFAULTS`. Because the profile is otherwise hydrated and initialized, the next write-behind flush unconditionally UPSERTs preferences for every dirty profile, permanently resetting the player's actual `player_preferences` row to defaults.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Track preference-load success separately (e.g., a `preferencesLoaded` flag) and only include a profile's preferences in the flush once they load successfully.
- [ ] Mirror the uninitialized-profile guard used for XP so a failed prefs load never overwrites persisted rows.
- [ ] Add a unit test: hydration with a prefs-read failure leaves the DB row intact after a flush.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java:304-318` (`loadPreferences` swallows `Exception`), `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java:146-152` (unconditional preferences UPSERT for every dirty profile), `PlayerPreferences.java`.
- **Dependencies:** None.
- **Constraints:** Preserve the write-behind pattern; do not add main-thread DB I/O.

## Verification & Definition of Done
- [ ] New test proves prefs are not overwritten after a load failure.
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: a later successful prefs load resumes normal flushing.
