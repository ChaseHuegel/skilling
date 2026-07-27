# ISSUE-002: Main-Thread Database I/O Blocking

## Description

Several code paths perform synchronous SQLite I/O on the Bukkit main thread,
contradicting the AGENTS.md rule: **"Never block the Bukkit Main Thread."**
This causes server tick freezes proportional to the number of dirty profiles
being flushed.

## Affected Code Paths

| File | Line | Context |
|------|------|---------|
| `PlayerListener.java` | 40 | `onPlayerQuit` calls `flushDirtyProfiles()` sync |
| `LockdownManager.java` | 63 | Phase 3 of `/skills reload` calls `flushDirtyProfiles()` sync |
| `AsyncBatchWorker.java` | 80–119 | `flushDirtyProfiles()` itself — no connection contention protection |
| `DatabaseManager.java` | 58 | `PRAGMA journal_mode=WAL` return value unchecked |

## Proposed Solution

### 1. Make player quit async

Replace synchronous flush in `PlayerListener.onPlayerQuit` with a
`CompletableFuture.runAsync(...)` that submits the flush to the
async worker's existing executor. Add a short (1 second) `join()`
with timeout so the player's quit event isn't held indefinitely.

### 2. Protect reload flush with a lock

Add a `ReentrantLock` to `AsyncBatchWorker` that guards
`flushDirtyProfiles()`. The async scheduler task acquires the
lock with `tryLock()` (skip if busy). The reload/quit paths
acquire the lock with `lock()` (block until done).

### 3. Verify WAL mode

In `DatabaseManager.initialize()`, execute
`PRAGMA journal_mode=WAL` and check the result set. Log a
warning if WAL mode was not actually enabled.

### 4. Clean WAL on shutdown

Add `PRAGMA wal_checkpoint(TRUNCATE)` to `shutdown()`.

## Steps

1. Add `ReentrantLock` to `AsyncBatchWorker`.
2. Guard `flushDirtyProfiles()` body with the lock.
3. Use `tryLock()` in the scheduled tick task.
4. Change `PlayerListener.onPlayerQuit` to submit async.
5. Verify `LockdownManager` main-thread flush — this one is
   acceptable during reload (server is frozen anyway), but
   document the intentional sync call.
6. Fix WAL verification in `DatabaseManager`.
7. Build, test, commit.

## Risk

Medium. The async quit path introduces timing considerations — a player
might reconnect before their quit flush completes. The dirty flag should
survive (it's still set if the flush didn't run), so the re-join
hydration will pick up any unflushed data.
