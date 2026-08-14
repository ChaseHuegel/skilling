# ISSUE-297: Database/profile minor fixes — leaks, shutdown ordering, and silent data-loss edges

## Context & User Story
- **Goal:** As a maintainer, I want a batch of small database/profile robustness fixes, each with a bounded scope.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] **`sessionGenerations` map never evicted:** `ProfileManager.java:29,70,191` grows by one entry per UUID on every `loadProfile`/`noteFlushCompleted` and entries are never removed. Remove the entry when a profile unloads with no reconnect, or prune entries whose profiles are absent.
- [ ] **`savePreferences` is dead code and a main-thread hazard:** `ProfileManager.java:326-337` performs synchronous JDBC and swallows all exceptions. Remove it, or rework it onto the write-behind path; never leave a blocking DB method with a silent catch.
- [ ] **`onDisable` timeout then pool close loses in-flight flushes:** `Skilling.java:721-731` awaits `flushDirtyProfilesAsync().get(5s)` and then closes the pool unconditionally; a slow flush that outlives the timeout fails with "pool not initialized". Await the flush future before closing the pool, or track completion explicitly.
- [ ] **Flush `tryLock()` silently skips retries:** `AsyncBatchWorker.java:74-80` uses `tryLock()`; if a quit-flush holds the lock the scheduled run is skipped, so a failed in-flight flush is not retried for up to 30s. Queue a deferred flush when the lock is contended.
- [ ] **`addXp` negative-sum saturation:** `PlayerProfile.java:133-137` clamps a negative `sum` to `Long.MAX_VALUE`. Return `Math.max(0, sum)` for negative results while keeping the positive-overflow clamp.
- [ ] **Uninitialized-profile session XP silently dropped:** when hydration fails, `ProfileManager` installs a placeholder; on quit the dirty uninitialized profile is unloaded and earned XP vanishes with only a `SEVERE` log at hydration time. Log a prominent warning on quit when dirty progress is dropped.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java:29,70,122-133,191,284-293,304-337`, `src/main/java/io/github/chasehuegel/skilling/engine/profile/PlayerProfile.java:133-137`, `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java:74-80`, `src/main/java/io/github/chasehuegel/skilling/Skilling.java:721-731`, `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java:81-111`.
- **Dependencies:** None.
- **Constraints:** Never block the Bukkit main thread. Preserve the write-behind cache pattern and the uninitialized-profile guard (it prevents overwriting real persisted rows).

## Verification & Definition of Done
- [ ] Each sub-item has a test where feasible (generation eviction, shutdown ordering, retry-after-contention, saturation).
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: normal save/quit flows produce identical persisted data.
