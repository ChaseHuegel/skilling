# ISSUE-240: BossBarPool lifecycle — hide pooled bars on disable and document the main-thread requirement

## Context & User Story
- **Goal:** As a server owner, I want `/reload` and plugin disable to not leave frozen XP boss bars floating over players, and I want the pool's public API to be safe for its advertised callers.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — two related lifecycle issues.

## Implementation Requirements
- [ ] `Skilling.onDisable()` (`Skilling.java:651-676`) never hides the bars owned by `BossBarPool`; after `/reload` the old pool's bars stay registered and visible (frozen, since the tick loop is gone). Call `removeAll`/hide every pooled bar in `onDisable()` before shutdown.
- [ ] `BossBarPool` calls main-thread-only Bukkit BossBar APIs (`Bukkit.createBossBar`, `addPlayer`, `hideBar`) while holding a lock (`BossBarPool.java:71-98`, `106-134`), but its Javadoc (`:17-19`) explicitly invites async callers via `SkillingAPI.getBossBarPool()`. Either route the API through a main-thread scheduler handoff or document the main-thread requirement and enforce it (Paper's `Player.getScheduler` / `Bukkit.getServer().getScheduler()`).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/BossBarPool.java`
  - `src/main/java/io/github/chasehuegel/skilling/api/SkillingAPI.java` (document threading if chosen)
  - `src/test/java/io/github/chasehuegel/skilling/feedback/BossBarPoolTest.java`
- **Dependencies:** none.
- **Constraints:** `bossbar.max_active` and `fade_ticks` behavior (including ISSUE-245) must stay consistent.

## Verification & Definition of Done
- [ ] No pooled boss bar survives `onDisable()`/reload.
- [ ] The pool's public entry points either run on the main thread or the Javadoc states the requirement clearly.
- [ ] `./gradlew build` and `./gradlew test` pass.
