# ISSUE-287: SQLite multi-writer pool with no busy-timeout tuning causes intermittent "database is locked"

## Context & User Story
- **Goal:** As a server owner, I want the database to stay available under load. The Hikari pool defaults to `database.pool_size` = 10 connections for a SQLite file that permits exactly one writer; concurrent writers (periodic flush, quit flushes, offline admin commands) contend for the write lock with no `busy_timeout`, surfacing as `SQLITE_BUSY` ("database is locked"). The flush logs `SEVERE` and retries 30s later; offline commands fail visibly.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Set `PRAGMA busy_timeout` (e.g. 5000ms) via Hikari `connectionInitSql` alongside `foreign_keys`.
- [x] Reduce writer contention: either cap the pool at one writer for SQLite or route all writes through a single serialized writer path.
- [x] Confirm WAL mode (`synchronous=NORMAL` if acceptable) and add a concurrency smoke test that hammers flush + quit + offline writes without a lock error.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/db/DatabaseManager.java:23-35` (pool config, init SQL), `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java` (flush loop). `ProfileManagerRaceTest` already simulates a `SQLITE_BUSY` hydration failure.
- **Dependencies:** `HikariCP 6.3.0`, `sqlite-jdbc 3.49.1.0`.
- **Constraints:** Never block the Bukkit main thread. Preserve the write-behind flush cadence.

## Verification & Definition of Done
- [x] Concurrency smoke test runs without `SQLITE_BUSY` under simultaneous writers.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: the periodic flush retries quickly after a lock instead of waiting a full interval.
