# ISSUE-291: Reload success clears the staging directory and wipes edits made while the reload was in flight

## Context & User Story
- **Goal:** As an admin, I want an edit I save while a reload is rebuilding to survive, not to be silently discarded. The reload flow applies staged changes, releases the staging lock, then runs an async lockdown rebuild that can take seconds; `stagingManager.clear()` on success deletes every staged file. A PUT/POST/DELETE staged during that window is wiped, while the response says "Changes applied. Plugin reloaded successfully."
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Stop clearing the whole staging directory on reload success. Capture the applied file list from `applyAndBackup()` and remove only those applied entries (and their staged sources) on success.
- [ ] Alternatively, hold a "reload in progress" state that rejects/blocks new stage requests until the rebuild completes.
- [ ] Add a test: stage an edit during the apply→clear window and assert it is preserved after the reload completes.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:47-97` (esp. `clear()` at ~85), `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:148-152,191-281` (`clear()` and `applyAndBackup()`).
- **Dependencies:** None.
- **Constraints:** Preserve the atomic-write and conflict-fingerprint behavior of `StagingManager`. Backups must still be kept.

## Verification & Definition of Done
- [ ] New in-flight-edit preservation test passes.
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: a normal single-admin flow (no concurrent edit) clears staging as before.
