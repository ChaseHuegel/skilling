# ISSUE-108: Ensure thorough, useful event logging when debug_logging is enabled

**Status:** Open
**Type:** Improvement
**Severity:** Low (troubleshooting aid)

---

## Context & User Story

- **Goal:** As a server admin, I want clear, consistent debug output when `debug_logging: true` is set, so that I can troubleshoot XP gains and ability behavior without guessing.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Ensure every XP/ability event path logs at least one structured line when `debug_logging` is enabled
- [x] Standardize the `[DEBUG]` prefix and use the existing `debug(...)` helper consistently
- [x] Cover the main event lifecycle: trigger fired, filters matched/skipped, XP granted (source, base, modifier multipliers, total), ability checked (locked/trigger mismatch), mechanic execution result

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (`debug` helper, lines 670-674; `grantXp`, `fireAbilities`, `dispatch`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/LevelUpDispatcher.java` (boss bar and unlock debug, lines 66-73, 146)
  - `src/main/java/io/github/chasehuegel/skilling/engine/LockdownManager.java` (reload phases)
- **Dependencies:** Config key `debug_logging` (`src/main/resources/config.yml`), read via `Skilling.isDebugLogging()`.
- **Constraints:** Debug logging must never fire on the hot path when disabled (guard before string building). Do not spam the normal info log with debug-level detail.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass
- [x] Runtime check: with `debug_logging: true`, earning XP and triggering an ability produce useful `[DEBUG]` lines covering the event lifecycle
- [x] Runtime check: with `debug_logging: false`, no `[DEBUG]` lines appear and no strings are built for them
