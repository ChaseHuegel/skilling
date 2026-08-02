# ISSUE-151: Make reload apply atomic and fail-safe (no permanent plugin freeze)

**Status:** Resolved
**Type:** Bug
**Severity:** High (failed reload can leave the plugin permanently locked)

---

## Context & User Story

- **Goal:** As a server owner, I want a failed "Apply & Reload" to leave the plugin functional, not stuck in a permanent reload/freeze state with interactions locked.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Wrap `LockdownManager.reload()` phases 5-6 (and any throw point such as `GuiLayoutConfig.load()`) in try/finally so `setReloading(false)` always runs
- [x] Ensure the apply step is atomic or restorable: if a mid-loop `Files.copy` fails, earlier applied files and staging are not left in a mixed state without a clear error + rollback path
- [x] On reload failure, report an accurate error to the admin and preserve the remaining staging for retry instead of clearing it
- [x] Add tests covering: `GuiLayoutConfig.load()` throwing does not leave the plugin reloading; a mid-apply copy failure does not clear remaining pending changes

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:89-97` (Phase 5/6, `GuiLayoutConfig.load()` outside the Phase-4 try)
  - `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:153-228` (`applyAndBackup` non-atomic copy)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:40-79`
- **Dependencies:** ISSUE-150 (backups) provides the rollback source for a restore-on-failure strategy.
- **Constraints:** `/skills reload` lockdown must still run its full sequence on success. Never leave `setReloading(true)` outstanding.

### Root Cause

`applyAndBackup()` copies files one-by-one with no transaction; if `Files.copy` throws mid-loop, earlier files stay applied and `ReloadHandler` proceeds to clear staging → mixed live state and lost pending edits. Worse, if `LockdownManager.reload()` throws (e.g. `GuiLayoutConfig.load()` raising `IllegalArgumentException` outside the Phase-4 try), the exception propagates to `ReloadHandler` which reports 500, but `setReloading(false)` (Phase 6) is never reached — the plugin stays frozen until a manual successful reload.

### Proposed Fix

Use try/finally around all reload phases so the unlock always runs. Make the apply step all-or-nothing (stage to a temp tree and atomically move, or restore from the ISSUE-150 backup on failure), and only clear staging after a fully successful reload.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Test: a reload that throws mid-way never leaves `isReloading()` true
- [x] Test: mid-apply failure preserves remaining pending changes and reports an error
- [x] Manual smoke: force a bad gui layout, reload, confirm the plugin stays interactive and reports the error
