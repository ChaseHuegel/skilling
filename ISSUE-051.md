# `AsyncBatchWorker` 240-second (4 minute) persistence interval — evaluate reducing for lower data-loss risk

## Issue

`AsyncBatchWorker.INTERVAL_TICKS` is set to `20 * 240` (line 18), which means dirty profiles are flushed to the database every 240 seconds (4 minutes). If the server crashes between flushes, up to 4 minutes of XP gains and skill progress can be lost.

**ISSUES.md reference:** Line 307

## Root Cause

The interval was set conservatively to avoid DB write contention. With SQLite in WAL mode and HikariCP connection pooling, write contention is minimal — the batch flush is also guarded by a `ReentrantLock` and skips if already running (`tryLock()`).

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `AsyncBatchWorker.java` | `engine/db/AsyncBatchWorker.java` | 18 |

## Development Plan

### Step 1: Evaluate trade-offs

| Interval | Data-loss window | Writes/second | Contention risk |
|----------|-----------------|---------------|-----------------|
| 240s (current) | 0-240s | Very low | None |
| 60s | 0-60s | Low | Low |
| 30s | 0-30s | Moderate | Low |
| 15s | 0-15s | Higher | Low (single-threaded) |
| 5s | 0-5s | High | Negligible with lock |

### Step 2: Reduce to 30 seconds (recommended)

```java
private static final long INTERVAL_TICKS = 20 * 30;
```

This provides a good balance: 30s data-loss window, ~2 DB writes/minute per dirty profile, and minimal contention since the flush is already locked.

### Step 3: Make interval configurable (optional)

Add a config key to `config.yml`:
```yaml
database:
  flush_interval_seconds: 30
```

Read in `Skilling.onEnable()` and pass to `AsyncBatchWorker`:
```java
int flushInterval = config.getInt("database.flush_interval_seconds", 30);
this.asyncBatchWorker = new AsyncBatchWorker(this, databaseManager, profileManager, flushInterval);
```

## Self-Review

- 4 minutes is excessive for an RPG skills plugin where players expect progress to persist
- 30 seconds is reasonable for most servers — balances data safety vs. write overhead
- Making it configurable is ideal but adds scope; fixed 30s is the minimal fix
- The `tryLock()` guard already prevents concurrent flushes, so more frequent runs are safe
- Update `docs/configuration.md` if a config key is added
