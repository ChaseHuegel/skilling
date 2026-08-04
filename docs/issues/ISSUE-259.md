# ISSUE-259: Quit-flush hydration race silently loses XP on quit→immediate-rejoin

## Context & User Story
- **Goal:** As a player, I want to log out and immediately back in during a heavy session without any XP earned in the last seconds being permanently lost.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — data loss. The session-generation guard covers eviction but not the install/replace decision.

## Implementation Requirements
- [ ] Prevent `installHydrated` from replacing a clean initialized profile with a stale DB snapshot while that profile's quit-flush is still in flight (e.g. capture the session generation at hydration start and skip the replace when it changed, or coordinate per-UUID so hydration and quit-flush serialize).
- [ ] Add a unit test that simulates hydration racing an in-flight flush and asserts the newer in-memory XP is not clobbered by the DB snapshot.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java:99-131` (`installHydrated` replaces a clean initialized profile with the snapshot)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java:77-92` (quit: async flush, then generation-guarded `unloadProfile`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java:104-167` (write-behind flush)
  - `src/test/java/io/github/chasehuegel/skilling/profile/ProfileManagerRaceTest.java`
- **Dependencies:** none.
- **Constraints:** Do not break the existing dirty-protection semantics: a *dirty* in-memory profile must still never be replaced by the snapshot. Reuse the existing `sessionGenerations` mechanism rather than adding a second concurrency model.

## Verification & Definition of Done
- [ ] Rejoin-during-quit-flush never results in the DB snapshot overwriting newer in-memory XP.
- [ ] Dirty-profile protection still holds.
- [ ] `./gradlew build` and `./gradlew test` pass.
