# ISSUE-251: LevelThresholds global lock on the hot level-lookup path

## Context & User Story
- **Goal:** As an engine maintainer, I want level computation to stay contention-free on the event path for a 20-TPS engine.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low (performance) — `LevelThresholds.table` takes a single global `synchronized (LOCK)` on a static `WeakHashMap` on every call (`LevelThresholds.java:41-50`), and `getLevelForXp` calls it on the hot path (per XP source, per ability, per PAPI placeholder). Correct under concurrency, but one global lock across all skills is a throughput smell.

## Implementation Requirements
- [x] Replace the global-lock `WeakHashMap` with a `ConcurrentHashMap` keyed by evaluator (per-evaluator `computeIfAbsent`, double-checked inside a `ConcurrentHashMap.compute` or `ConcurrentSkipListMap`), preserving the weak-collection-on-reload behavior (or document an alternative invalidation that keeps reloads safe).
- [x] Add a concurrency test proving parallel `getLevelForXp` calls across distinct skills do not serialize.

## Technical Specifications & Context
- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/LevelThresholds.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/...` (concurrency test)
- **Dependencies:** none.
- **Constraints:** Behavior must be bit-for-bit identical; reloads still evict stale tables (a new evaluator instance keys a new table — verify that still holds with the new structure).

## Verification & Definition of Done
- [x] No global lock remains on the lookup path.
- [x] Reload invalidation still works.
- [x] `./gradlew build` and `./gradlew test` pass.
