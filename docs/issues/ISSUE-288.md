# ISSUE-288: `LevelThresholds` mutates the map inside `ConcurrentHashMap.computeIfAbsent`

## Context & User Story
- **Goal:** As a maintainer, I want reloads and hot-path lookups to be safe. `LevelThresholds.table()` runs `CACHE.keySet().removeIf(...)` inside the mapping function of `CACHE.computeIfAbsent(...)` on the same map. The CHM contract forbids updating the map from its own mapping function; this can livelock or corrupt the table during `/skills reload`, which is exactly when old evaluators are cleared. The call is on the hot `getLevelForXp` path.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Evict cleared keys outside the mapping function (e.g., before the `computeIfAbsent` call in `table()`), or switch to a bounded CHM with explicit eviction, or guard eviction with a lock.
- [ ] Add a stress test: repeated concurrent `getLevelForXp` while a reload clears the cache completes without hanging or corrupting thresholds.

## Technical Specifications & Context
- **Target Files:** `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/LevelThresholds.java:55-59` (the offending block), tests at `skilling-api/src/test/java/io/github/chasehuegel/skilling/engine/LevelThresholdsTest.java`. Callers: `getLevelForXp` from event handlers and the async worker.
- **Dependencies:** None.
- **Constraints:** Keep lookups O(log n) and off the mutation of live maps from concurrent readers.

## Verification & Definition of Done
- [ ] Stress test passes without hang/corruption.
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: reload rebuilds thresholds while players are earning XP.
