# ISSUE-117: Guard `ProjectileHitEvent` handler against non-`LivingEntity` hits

**Status:** Open
**Type:** Bug
**Severity:** Critical (unhandled `ClassCastException` in a HIGHEST-priority main-thread handler)

---

## Context & User Story

- **Goal:** As a server owner, I want the plugin to handle projectiles hitting item frames and paintings gracefully so the event chain is not broken for the whole server.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Replace the unconditional cast to `LivingEntity` with an `instanceof LivingEntity` guard in the projectile-hit handler
- [ ] Ensure a non-`LivingEntity` hit (item frame, painting, armor stand edge cases) safely returns without dispatching abilities
- [ ] Add a unit test exercising a hit on a `Hanging` entity that does not throw

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:599-605`
- **Dependencies:** None. The handler is registered at `HIGHEST` (`SkillEventListener.java:146-151` region) on the main thread.
- **Constraints:** Do not change the dispatch semantics for valid `LivingEntity` hits.

### Root Cause

The handler null-checks `getHitEntity()` but then unconditionally casts to `LivingEntity`. `ProjectileHitEvent.getHitEntity()` can return `Hanging` types such as `ItemFrame` or `Painting`. A Skilling projectile (e.g. snowball) hitting an item frame throws `ClassCastException` inside the HIGHEST handler, breaking that event's handling for the entire server.

### Proposed Fix

Guard with `if (!(event.getHitEntity() instanceof LivingEntity living)) return;` before dispatching.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: hit on a `Hanging` entity does not throw and dispatches nothing
- [ ] Unit test: hit on a `LivingEntity` still dispatches abilities
