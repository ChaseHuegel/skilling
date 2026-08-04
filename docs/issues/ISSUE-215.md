# ISSUE-215: Engine data-integrity hardening (cooldowns, offline grants, negative XP, DB init, flush interval)

## Context & User Story
- **Goal:** As a server admin, I want the engine to never lose or corrupt player data: cooldowns should not be trivially bypassable, offline admin grants should not be overwritten, negative XP must not crash the boss bar, DB init failure must stop the plugin, and the write-behind window should not lose 20 minutes of XP on a crash.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] **Cooldown bypass on relog:** `PlayerListener.onPlayerQuit` clears all ability cooldowns (`PlayerListener.java:66`). Preserve cooldown state across a quit/relog (persist remaining cooldowns, or move them into the profile write-behind) so logging out cannot reset an active cooldown.
- [x] **Offline admin grant TOCTOU:** `SkillsCommand.handleOfflineSetLevel`/`handleOfflineAddXp` (`SkillsCommand.java:303-318`, `364-379`) write directly to the DB; a concurrent login with a stale cached profile can overwrite the grant and clear `fanfare_pending=1` on the next flush (`AsyncBatchWorker.java:29`). Coordinate direct-DB writes with the write-behind cache (e.g. reject/refresh when the target has a live profile, or merge into the cached profile).
- [x] **Negative XP / boss-bar crash:** `LevelUpDispatcher.showXpBossBar` (`LevelUpDispatcher.java:58-61`) computes a negative `progress` when total XP is below the current level threshold and calls `bar.setProgress(negative)` -> IllegalArgumentException. Clamp progress to [0,1] and clamp/validate negative `addxp`/`setlevel` inputs (`SkillsCommand.java:176,189` use unbounded `IntegerParser`).
- [x] **DB init failure:** `Skilling.onEnable` logs and continues when `databaseManager.initialize` throws (`Skilling.java:175-180`), leaving the plugin running with zero persistence. Log SEVERE and disable the plugin (or refuse to enable) on init failure.
- [x] **Flush interval:** `AsyncBatchWorker.INTERVAL_TICKS = 20 * 60` (`AsyncBatchWorker.java:20`) is a 20-minute crash-loss window. Shorten the periodic flush interval to a defensible bound (e.g. 1-2 minutes) without hurting 20-TPS performance, and ensure the first flush runs promptly rather than 20 minutes after enable.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/LevelUpDispatcher.java`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java`
  - Tests: `RequirementsEngineCooldownTest`, `ProfileManagerRaceTest`, `SkillsCommandPageCacheTest`, `BossBarPoolTest`
- **Dependencies:** none.
- **Constraints:** Do not block the main thread (see ISSUE-214). Preserve the fanfare-on-next-login flow for offline grants that land while the target is truly offline.

## Verification & Definition of Done
- [x] Relog does not reset an active cooldown.
- [x] Offline grant no longer lost when the target logs in during the write window; fanfare still fires for genuinely offline targets.
- [x] Negative `addxp`/`setlevel` cannot crash the boss bar or corrupt XP.
- [x] Plugin refuses to enable when the DB cannot initialize.
- [x] Crash-loss window reduced to the new interval.
- [x] `./gradlew build` and `./gradlew test` pass.
