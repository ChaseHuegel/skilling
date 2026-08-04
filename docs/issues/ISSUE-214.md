# ISSUE-214: Reload and shutdown must not block the Bukkit main thread

## Context & User Story
- **Goal:** As a server admin, I want `/skills reload` and plugin shutdown to never freeze the server tick for up to 5 seconds, and the web reload request to never hang a worker thread indefinitely.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Remove the up-to-5-second main-thread block in `LockdownManager.reload` phase 3 (`LockdownManager.java:71`, `flushDirtyProfilesAsync().get(5, SECONDS)`). Either flush without blocking (the freeze already short-circuits event dispatch), or continue the reload asynchronously after the flush completes with a bounded wait that is not on the main thread.
- [x] Bound the web reload path: `ReloadHandler.reload` (`ReloadHandler.java:57-63`) uses `callSyncMethod(...).get()` with no timeout on a Jetty thread — add a bounded timeout and surface failure to the admin instead of hanging.
- [x] Revisit `onDisable` (`Skilling.java:632`): the shutdown flush also blocks up to 5s on the main thread. If a bounded wait on disable is acceptable, document it; otherwise drain without the main-thread block.
- [x] Ensure the phase-3 flush "fully persisted" invariant that phases 4-5 rely on (`markSaved` logic, `LockdownManager.java:106-118`) is preserved or reworked under the non-blocking design. Track the flush future and gate the rebuild/markSaved phases on its completion.
- [x] Add/adjust tests in `LockdownManagerReloadTest` / `WebReadConcurrencyTest` covering the bounded-wait behavior.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java` (lines 66-76, 106-118)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java` (lines 55-69)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (lines 626-636)
  - `src/test/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManagerReloadTest.java`
- **Dependencies:** `AsyncBatchWorker.flushDirtyProfilesAsync`.
- **Constraints:** `src/AGENTS.md` §2 — never block the Bukkit main thread. Bounded waits on non-main threads are acceptable.

## Verification & Definition of Done
- [x] `/skills reload` no longer stalls the server tick on the flush.
- [x] A stalled main thread during web reload produces a bounded error response, not a hung worker.
- [x] Flush-before-rebuild ordering and the markSaved invariant are preserved (no lost updates).
- [x] `./gradlew build` and `./gradlew test` pass.
