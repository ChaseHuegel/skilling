# ISSUE-143: Consume `fanfare_pending` on profile load so offline admin XP/level changes get fanfare

**Status:** Resolved
**Type:** Bug
**Severity:** High (documented feature is dead; flag never clears)

---

## Context & User Story

- **Goal:** As an admin, I want offline `/skills setlevel`/`addxp` to show the player fanfare on their next login, as the command contract documents.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Read the `fanfare_pending` flag during profile load; when set, fire the configured level-up/fanfare on the player's first join and clear the flag
- [x] Ensure the batch worker's `ON CONFLICT DO UPDATE` also resets `fanfare_pending` to 0 after it is consumed (currently only `xp` is updated, so the flag persists forever)
- [x] Preserve offline admin changes against the login/flush race (coordinate with ISSUE-112)
- [x] Add a test covering: offline setlevel sets the flag, profile load reads and clears it, fanfare fires on next login

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java:301,361`
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java:51-64`
  - `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java:67-71`
- **Dependencies:** ISSUE-112 (login/hydration race) and the fanfare dispatchers.
- **Constraints:** Do not re-fire fanfare on every subsequent login; the flag must be consumed exactly once.

### Root Cause

The offline handlers set `fanfare_pending = 1`, but no code reads the flag on profile load, so the "fanfare on next login" behavior never fires. Worse, the batch worker's `ON CONFLICT DO UPDATE SET xp = excluded.xp` only touches `xp`, leaving `fanfare_pending` set in the DB forever. Offline admin XP/level changes apply with no level-up feedback, and the flag accumulates stale rows.

### Proposed Fix

Hydrate the flag in `ProfileManager.loadProfile`; on first join, fire fanfare for the applied skill/level and clear the flag (in memory and via the next flush). Add `fanfare_pending = 0` (or `excluded.fanfare_pending`) to the UPSERT.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new regression test
- [x] Unit test: offline setlevel flags the row; profile load reads and clears it
- [x] Unit test: fanfare fires once on the next login, not again on later logins
- [x] DB review: after a flush, `fanfare_pending` is cleared
