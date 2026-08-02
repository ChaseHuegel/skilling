# ISSUE-156: Make `StagingManager` thread-safe with atomic writes and conflict-detection fixes

**Status:** Resolved
**Type:** Bug
**Severity:** High (lost updates, torn file copies, backup collisions)

---

## Context & User Story

- **Goal:** As an admin, I want concurrent web edits and reloads to never lose pending changes or copy a truncated YAML file into the live config.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Serialize `StagingManager` mutations (edits, status updates, apply, clear) with a lock so concurrent PUTs cannot lose entries or race `clear()`
- [x] Make all file writes atomic (write temp file + atomic move) so a concurrent reload never copies a partial file
- [x] Make `status.json` updates atomic and read-modify-write safe
- [x] Use collision-free backup directory naming (e.g. nanos/UUID) instead of `LocalDateTime.now()` so concurrent reloads cannot overwrite each other's backups
- [x] Fix conflict-detection blind spots: snapshot/verify new files that did not exist at staging time, avoid mtime-granularity misses, and close the TOCTOU window between conflict check and copy
- [x] Close the unclosed `Files.walk` stream in `clear()`
- [x] Add tests covering: concurrent PUTs lose no entries; reload cannot read a torn file; two concurrent reloads produce distinct backups

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:54-70,81-125,153-292`
- **Dependencies:** ISSUE-150/151 (backup + reload atomicity) overlap.
- **Constraints:** Javalin serves requests on many Jetty threads; every `StagingManager` entry point must be safe.

### Root Cause

No locking exists anywhere. `updateStatusAdd` does read-modify-write on `status.json` (concurrent PUTs lose entries); `Files.writeString` is not atomic (a concurrent reload can copy a truncated file into live config); two concurrent reloads in the same second share one backup directory and overwrite each other's backups. Conflict detection only snapshots existing files, misses same-second/mtime-preserving edits, and leaves a TOCTOU window before the copies.

### Proposed Fix

Add a single lock (or synchronized methods) guarding all staging mutations, write via temp+atomic-move, use unique backup names, and record content-hash/fingerprints (not just mtimes) for conflict detection over existing and new files.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new concurrency tests
- [x] Test: 10 concurrent PUTs produce 10 distinct staged files and a correct status
- [x] Test: a staged write concurrent with reload never yields a partial live file
- [x] Test: concurrent reloads produce distinct backups
- [x] Resource review: no unclosed streams in `clear()`
