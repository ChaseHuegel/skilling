# ISSUE-120: Fix `XpBonusMechanic` permanent multiplier (static map never cleared)

**Status:** Open
**Type:** Bug
**Severity:** High (one activation applies to all future XP forever + memory leak)

---

## Context & User Story

- **Goal:** As a player, I want the XP bonus from an ability to last only for its intended duration, not silently boost every future XP gain for the server's lifetime.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Make the multiplier expire (TTL) after its configured duration instead of persisting for the whole session/server life
- [ ] Clear the player's multiplier entry on quit, on profile unload, and on plugin reload
- [ ] Guard against re-entrant/extended application (an activation refreshes the TTL, does not stack)
- [ ] Add unit tests covering: expiry after duration, quit clears the entry, repeated activation refreshes rather than stacks

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/XpBonusMechanic.java:20,33` (`multipliers` static map)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/PlayerListener.java:36-56` (quit cleanup)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:388-390` (hot-path read)
- **Dependencies:** The static `ConcurrentHashMap<UUID, Double>` is read on every XP grant.
- **Constraints:** Keep the read on the XP hot path cheap. Do not introduce per-event allocations.

### Root Cause

The `multipliers` map is written on activation and never removed — not on quit, not on expiry, not on reload. A one-time activation applies the multiplier to all future XP for the server's lifetime (contrary to the "session" Javadoc) and leaks a map entry per player.

### Proposed Fix

Store `(multiplier, expiryNanos)` per UUID, check/evict expiry on read, and clear the entry in `onPlayerQuit` and `onDisable`/reload. Optionally schedule a delayed removal task mirroring the attribute-modifier mechanics.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: multiplier no longer applies after its duration elapses
- [ ] Unit test: quit removes the player's entry
- [ ] Unit test: re-activation refreshes the TTL without stacking
