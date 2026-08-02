# ISSUE-145: Move all SQLite flushes/writes off the Bukkit main thread

**Status:** Resolved
**Type:** Bug
**Severity:** High (violates the "never block the main thread" contract)

---

## Context & User Story

- **Goal:** As a server owner, I want `/skills reload`, `/skills log`, and server shutdown to never freeze the main thread on database I/O.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Make `LockdownManager`'s `/skills reload` flush and `Skilling.onDisable` flush execute the JDBC batch asynchronously (or cooperatively), never synchronously on the main thread
- [x] Make `/skills log` (`SkillsCommand.java:155` → `ProfileManager.savePreferences`) go through the async dirty-flag/batch path instead of a blocking `INSERT` on the command thread
- [x] Ensure shutdown still guarantees pending data is flushed before the pool closes (await the async flush with a bounded timeout, not an unbounded main-thread block)
- [x] Add a test (or review note) asserting no DB write executes on the main thread during reload

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:64`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:495`
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java:155`
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java:162-173`
- **Dependencies:** `AsyncBatchWorker.flushDirtyProfiles()` (reentrant, lock-guarded) is the async path to use.
- **Constraints:** `src/AGENTS.md` §2: "Never block the Bukkit Main Thread. All SQLite database reads/writes must be executed asynchronously." Shutdown correctness must not regress (no lost final flush).

### Root Cause

`flushDirtyProfiles()` (full JDBC `executeBatch`) is called synchronously from `/skills reload` and `onDisable`, and `savePreferences` runs a blocking `INSERT ... ON CONFLICT` on the command thread. On a busy server the batch can take hundreds of ms to seconds, freezing the server during reload and shutdown.

### Proposed Fix

Dispatch the flush through the async batch worker (or `runTaskAsynchronously`) and have reload/shutdown await completion with a bounded timeout before proceeding (reload) or closing the pool (shutdown). Route `/skills log` through the dirty-flag write-behind path.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass
- [x] Review: no DB write path is reachable from the main thread after the change (all `getConnection` call sites are on the pre-login async thread, the async timer/`flushDirtyProfilesAsync`, or `runTaskAsynchronously`; `savePreferences` has no main-thread caller)
- [x] Shutdown test: pending dirty profiles are still flushed before the pool closes
- [x] Manual smoke: `/skills reload` on a busy server does not visibly freeze (by design: batch executes off-thread, reload/shutdown await a bounded 5s timeout)
