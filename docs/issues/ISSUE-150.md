# ISSUE-150: Preserve backups after a reload instead of wiping them in `clear()`

**Status:** Open
**Type:** Bug
**Severity:** High (documented rollback feature never survives a reload)

---

## Context & User Story

- **Goal:** As an admin, I want the backup created before "Apply & Reload" to still exist afterward, so I can roll back a bad apply.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Stop `ReloadHandler.reload()` from destroying the backup it just created on success
- [ ] Make `StagingManager.clear()` (and `DELETE /api/staging`) preserve `.web_staging/backup/` (clear only staged pending edits)
- [ ] Ensure backup directory naming cannot collide (two reloads in the same second) — coordinate with ISSUE-156
- [ ] Add a test asserting a backup exists and is intact after a successful reload

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:65`
  - `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:114-125,165-167`
- **Dependencies:** `web/AGENTS.md:72,114` documents backups as part of the apply workflow.
- **Constraints:** Do not keep stale pending edits around after discard; only the backup tree survives.

### Root Cause

`applyAndBackup()` creates `staging/backup/{timestamp}/`, then `ReloadHandler.reload()` calls `stagingManager.clear()` on success — and `clear()` deletes the entire `.web_staging` tree, including the backup just written. The documented backup feature never survives a reload, so there is no rollback path. `DELETE /api/staging` has the same effect.

### Proposed Fix

Exclude `.web_staging/backup/` from `clear()`. Optionally add a `DELETE /api/staging/backups` or TTL to manage old backups.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Test: after a successful reload, `backup/{timestamp}/` contains the pre-apply files
- [ ] Test: `DELETE /api/staging` removes pending edits but not backups
- [ ] Manual smoke: apply a change, verify backup, restore from it
