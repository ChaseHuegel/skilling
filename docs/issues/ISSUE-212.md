# ISSUE-212: DB hydration failure must not install an empty profile that overwrites persisted XP

## Context & User Story
- **Goal:** As a player, I want a transient database error during login to never reset my accumulated skill XP.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `ProfileManager.loadProfile` must not swallow a hydration SQL failure and install a marked-initialized empty profile. On failure, the profile must not be treated as authoritative, and a later write-behind flush must not UPSERT empty values over the player's real rows.
- [x] Decide the failure semantics: retry hydration (bounded), or fall back to an uninitialized in-memory-only profile that is never persisted unless it can read the DB first, or log SEVERE and leave the profile absent. Empty-on-error is unacceptable because `AsyncBatchWorker` unconditionally `DO UPDATE SET xp = excluded.xp` (`AsyncBatchWorker.java:29`), which overwrites the persisted XP once the player earns any XP.
- [x] Log the failure with the SQL exception (currently `catch (Exception ignored)` at `ProfileManager.java:70-72` and the preferences catch at `:187-189`).
- [x] Add a regression test that simulates a hydration failure and asserts the player's persisted XP is never overwritten by an empty-profile flush.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java` (lines 46-80, 176-190)
  - `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java` (lines 26-30, 92-150)
  - `src/test/java/io/github/chasehuegel/skilling/profile/ProfileManagerRaceTest.java` (or new test)
- **Dependencies:** none.
- **Constraints:** "database is locked" is realistic under load (WAL + HikariCP). The fix must preserve the fast path (no failure -> hydrate normally).

## Verification & Definition of Done
- [x] A simulated hydration failure never results in the persisted XP row being overwritten with 0 or a small session value.
- [x] New regression test passes.
- [x] `./gradlew build` and `./gradlew test` pass.
