# ISSUE-112: Make profile load/unload atomic on player reconnect to prevent XP loss

**Status:** Open
**Type:** Bug
**Severity:** High (race can evict a fresh profile or hydrate stale data on quick reconnect)

---

## Context & User Story

- **Goal:** As a player, I want a fast quit/reconnect (or a quit while an async flush is in flight) to never lose or overwrite my progression.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Make `ProfileManager.loadProfile` merge-or-replace safely so an async hydration can never clobber a dirty in-memory profile (e.g. `putIfAbsent`/merge semantics guarded by initialization state)
- [ ] Make the quit path (`PlayerListener.onPlayerQuit`) flush and remove the profile per-player and only when the cached entry is still the same instance being unloaded
- [ ] Ensure `unloadProfile` never evicts a profile that replaced the original entry (e.g. a new login) during an in-flight async flush
- [ ] Ensure `getOrCreate` never hands out an uninitialized (zero-XP) profile before hydration completes; gate XP reads or hydrate synchronously on the main thread when needed
- [ ] Add tests covering: reconnect during quit-flush, hydration arriving after a dirty profile exists, and uninitialized-profile reads

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/ProfileManager.java:42-96` (`loadProfile`, `getOrCreate`, `unloadProfile`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java:40-52` (`onPlayerQuit`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/db/AsyncBatchWorker.java:63-103` (`doFlush`)
- **Dependencies:** ISSUE-111 (flush `markSaved` semantics) should be fixed first; the reconnect race and the flush race compound.
- **Constraints:** Never block the main thread with DB I/O. The profile map is a `ConcurrentHashMap` keyed by `UUID`.

### Root Cause

`loadProfile` unconditionally does `profiles.put(uuid, profile)`, overwriting any existing entry. If the player reconnects while the quit-path async flush is still running, the async `whenComplete` handler calls `unloadProfile` which removes whatever entry is currently in the map — which may be the **new** login's profile. Separately, `getOrCreate` can return an empty, not-yet-hydrated profile whose XP reads as 0, so early event handlers grant/overwrite data before hydration lands.

### Proposed Fix

- In `loadProfile`, only install the hydrated profile if no newer dirty profile exists (`putIfAbsent`, or compare instance identity).
- Have `unloadProfile` accept the exact `PlayerProfile` instance to remove and skip removal if the current map entry differs.
- For `getOrCreate`, return the live profile and defer hydration-based overwrites; ensure callers that need initialized data await hydration (or hydrate synchronously when no cached profile exists).

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new race-condition tests
- [ ] Unit test: quit flush completing after a reconnect does not evict the new profile
- [ ] Unit test: async hydration does not overwrite XP mutated after load began
- [ ] Manual smoke: `/skills setlevel` on an offline player then immediate login shows the applied level and is not reverted by a stale flush
