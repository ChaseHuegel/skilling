# ISSUE-266: Apply-then-reload ordering leaves a false, permanent 409 conflict

## Context & User Story
- **Goal:** As an admin, I want a failed or timed-out reload to be retryable, not stuck on a bogus "live files modified since staging" conflict caused by my own Apply step.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — admin stuck with no path to retry; live files changed while the engine still runs the old config.

## Implementation Requirements
- [x] After a successful `applyAndBackup()`, refresh the `status.json` file fingerprints (re-snapshot the now-live files), or clear the conflict state on apply so the subsequent reload does not see the applied files as "modified since staging".
- [x] Add a unit test covering apply → reload-failure → retry-reload succeeding.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:84-107` (`writeStatus` snapshots live fingerprints), `:109-130` (`checkConflicts`), `:182-268` (`applyAndBackup` never refreshes fingerprints)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:49-80` (applies, then on timeout/failure preserves staging for retry)
  - `src/test/java/io/github/chasehuegel/skilling/web/staging/StagingManagerConcurrencyTest.java`
- **Dependencies:** none.
- **Constraints:** Preserve the genuine external-modification conflict detection (FTP-edited files must still 409). Only the self-inflicted post-apply state must stop conflicting. Staging is preserved on failure by design; the retry must work.

## Verification & Definition of Done
- [x] After a failed reload, a retry `POST /api/reload` no longer reports a false 409.
- [x] Externally-modified live files still trigger a real 409.
- [x] `./gradlew build` and `./gradlew test` pass.
