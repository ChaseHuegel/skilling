# ISSUE-245: `bossbar.max_active: 0` should disable the bar, not degrade to a cap of 1

## Context & User Story
- **Goal:** As a server owner, I want `bossbar.max_active: 0` to disable the XP boss bar as configured, not silently behave as a cap of 1.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `BossBarPool` with `maxActive == 0` finds no eviction candidate (`countForPrefix(prefix) >= maxActive` is `0 >= 0`, `eldest == null` breaks) and proceeds to create a bar anyway (`BossBarPool.java:79-92`).

## Implementation Requirements
- [x] Treat `maxActive <= 0` as "bars disabled": `getOrCreate` returns/exposes no bar and the pool never registers one.
- [x] Add a unit test asserting no bar is created when `max_active: 0`.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/BossBarPool.java`
  - `src/test/java/io/github/chasehuegel/skilling/feedback/BossBarPoolTest.java`
  - `src/main/resources/config.yml` (document `0` disables)
- **Dependencies:** none.
- **Constraints:** `max_active: 1`..N still works as today (see ISSUE-240 for lifecycle).

## Verification & Definition of Done
- [x] `max_active: 0` yields no boss bar at all.
- [x] Positive caps unchanged.
- [x] `./gradlew build` and `./gradlew test` pass.
