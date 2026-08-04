# ISSUE-242: `/skills set database.pool_size` reports success but has no effect until restart

## Context & User Story
- **Goal:** As an admin, I want `/skills set` to either apply a config change or explicitly tell me it requires a restart, so a reported success never silently does nothing.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `reloadConfigSettings()` (`Skilling.java:813-832`) only re-applies `bossbar.*`, `debouncer.interval_ms`, and `skills_guide_book.enabled`; the Hikari pool size was fixed at construction (`Skilling.java:178-185`). `/skills set database.pool_size N` saves the value and replies "Set database.pool_size to N" with zero effect until restart.

## Implementation Requirements
- [ ] Make `/skills set` (and the web config editor, if applicable) report that `database.pool_size` requires a restart instead of claiming success — or apply the change live (not feasible for a Hikari pool; prefer the clear restart-required message, mirroring the web `ConfigHandler` `WEB_RESTART_REQUIRED` behavior).
- [ ] Add a test asserting the restart-required path surfaces for pool-size changes.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java` (set handler, ~`:209-220`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/ConfigKeyParser.java`
  - `src/test/java/io/github/chasehuegel/skilling/command/...`
- **Dependencies:** none.
- **Constraints:** Keep the restart-applied keys (`bossbar`, `debouncer`, guide book) working as today.

## Verification & Definition of Done
- [ ] `/skills set database.pool_size N` no longer reports a success that has no effect.
- [ ] Live-applicable keys still apply.
- [ ] `./gradlew build` and `./gradlew test` pass.
