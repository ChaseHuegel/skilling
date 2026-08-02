# ISSUE-147: Make `/skills set` configurable keys actually take effect at runtime

**Status:** Open
**Type:** Bug
**Severity:** Medium (command reports success but nothing changes)

---

## Context & User Story

- **Goal:** As an admin, I want `/skills set bossbar.max_active 5` (and the debouncer/guide-book keys) to take effect, or be rejected as read-only — never silently accepted with no result.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Either rebuild/refresh `BossBarPool` and `FeedbackDebouncer` settings when the relevant config keys change, or remove those keys from the writable set and report them as read-only
- [ ] Make `SkillsGuideBook.enabled` respect config changes (currently a `final` captured at construction)
- [ ] Ensure `/skills set` and the web `PUT /api/config` (ISSUE-154) behave consistently for these keys
- [ ] Add a test (or documented behavior) covering: changing a supported key updates the subsystem; unsupported keys are rejected with a clear message

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java:196-209`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:197-201,601-608`
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/BossBarPool.java` and `FeedbackDebouncer.java` (final fields)
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillsGuideBook.java:42`
- **Dependencies:** ISSUE-142 (BossBarPool config) overlaps.
- **Constraints:** Fail-fast/clear-communication rather than silent no-ops.

### Root Cause

`/skills set bossbar.max_active 5` writes config.yml and calls `reloadConfigSettings()`, but `BossBarPool.maxActive`/`fadeTicks` and `FeedbackDebouncer.intervalMs` are `final` fields created once in `onEnable`, and `SkillsGuideBook.enabled` is `final` captured at construction. The command reports success but nothing changes.

### Proposed Fix

Make the affected subsystems rebuildable from config (mutable fields or a rebuild method) and invoke it from `reloadConfigSettings`, or narrow `ConfigKeyParser`'s writable set and reject unsupported keys with a clear message.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass
- [ ] Behavior test: `/skills set bossbar.max_active 5` changes the pool's active limit (or is rejected explicitly)
- [ ] Behavior test: guide-book enabled flag updates after config change
- [ ] `/skills set` output never claims success for a no-op change
