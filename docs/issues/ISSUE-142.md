# ISSUE-142: Fix `BossBarPool` LRU scope, locking, and non-applied config

**Status:** Open
**Type:** Bug
**Severity:** High (constant boss-bar churn, races, dead config contract)

---

## Context & User Story

- **Goal:** As a server owner, I want `bossbar.max_active` to mean bars per player's screen, not a server-wide limit that causes bars to flicker in and out on every XP gain.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Scope the LRU eviction per player (config/class contract: "Maximum active Boss Bars visible on a player's screen"), not globally across all players
- [ ] Hold the pool lock across the entire get-or-create (including eviction) so two concurrent `getOrCreate` calls for the same key cannot orphan a bar that is never hidden
- [ ] Make `tickAll()` iterate under the lock (or a thread-safe iteration) to avoid `ConcurrentModificationException`
- [ ] Coordinate eviction with TTL expiry so an evicted/hidden bar is not left visible detached from the cache
- [ ] Ensure `bossbar.max_active`/`fade_ticks` from config actually drive the pool (currently `final` fields created in `onEnable`; see ISSUE-147) or remove the dead config keys
- [ ] Remove the unused `BossBarPool.get()` or make it part of the tested contract
- [ ] Add tests covering: per-player LRU, concurrent get-or-create single bar, tickAll under concurrent access

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/feedback/BossBarPool.java:34-39,51-70,127-142`
- **Dependencies:** `SkillEventListener.grantXp` calls `showXpBossBar` on every XP grant (line 394), amplifying the churn. The pool is exposed via `SkillingAPI.getBossBarPool()` so addons can call from async threads.
- **Constraints:** Keep per-player screen real-estate bounded. All engine-side callers today are main-thread; the pool must also be safe for async addon callers.

### Root Cause

The LRU is keyed by `<uuid>:<skillId>` and evicts the eldest entry **across all players**. With the default `max_active: 2`, the entire server shares 2 bars: player B gaining XP evicts player A's bar, and A's next XP tick recreates it, evicting B — `createBossBar`/`addPlayer`/`hideBar` churn on every XP gain for 3+ concurrent players. `tickAll()` iterates the synchronized map without the lock (CME risk), and get-or-create is not atomic (two concurrent calls for the same key orphan a bar that stays visible forever).

### Proposed Fix

Key the LRU per player (or maintain a per-player bounded set), synchronize the full get-or-create and tickAll iteration, and coordinate TTL expiry with eviction so every bar removed is hidden and no bar is left detached-but-visible. Wire the config values through (see ISSUE-147).

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: `max_active: 2` with 3 players yields 2 bars per player, not 2 server-wide
- [ ] Unit test: concurrent get-or-create for the same key yields one active bar
- [ ] Unit test: tickAll under concurrent get/remove does not throw CME
- [ ] Manual smoke: 3+ players gaining XP shows no bar flicker
