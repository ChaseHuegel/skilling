# ISSUE-111: Fix `markSaved()` lost-update race that silently drops player XP

**Status:** Open
**Type:** Bug
**Severity:** Critical (silent XP data loss on the write-behind cache flush)

---

## Context & User Story

- **Goal:** As a player, I want all XP I earn to be durably persisted so that a periodic flush or a server quit never loses progression earned just before the save.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Capture the profile's modification counter at the time the XP snapshot is taken in the flush, and record that captured value as the saved marker instead of the live counter
- [ ] Change `PlayerProfile.markSaved()` to accept the snapshot-time marker (e.g. `markSaved(long snapshotModCount)`) so a profile that gained XP after the snapshot correctly remains dirty
- [ ] Update the class Javadoc on `markSaved()` so it accurately describes the retained-dirty semantics (the current Javadoc claims the opposite of the implementation)
- [ ] Add a unit test reproducing the race: snapshot XP, add XP concurrently, flush, assert the profile is still dirty and the next flush persists the new XP

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/PlayerProfile.java:178-193` (`isDirty`/`markSaved`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java:63-103` (`doFlush`)
- **Dependencies:** None. The `PlayerProfile` uses an `AtomicInteger modCount`; the flush snapshots XP via `getXpSnapshot()` (`PlayerProfile.java:142-144`).
- **Constraints:** Keep the flush non-blocking and off the main thread. Do not weaken dirty tracking.

### Root Cause

`doFlush` takes `profile.getXpSnapshot()` (a copy of the XP map), writes the batch, then calls `markSaved()`. `markSaved()` sets `savedModCount = modCount.get()` — the **current** counter, which includes any `addXp` that happened after the snapshot but before `markSaved()`. Because the DB write did not include that newer XP, the profile is marked clean while the delta exists only in memory. `isDirty()` then returns false, so no later flush persists it and the XP is lost on quit/restart.

### Proposed Fix

At snapshot time, also capture `modCount`. Pass that captured value into `markSaved()`, and have `markSaved(long snapshotModCount)` set `savedModCount = snapshotModCount`. Any `addXp` between snapshot and the write leaves `modCount > savedModCount`, so the profile stays dirty and the periodic flush (or the quit-path re-check) retries with fresh data.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: XP added between snapshot and `markSaved` keeps the profile dirty and the next flush persists it
- [ ] Unit test: a quiescent profile (no concurrent modification) is correctly marked clean after a successful flush
- [ ] Code review confirms no other caller relies on the zero-arg `markSaved()` semantics
