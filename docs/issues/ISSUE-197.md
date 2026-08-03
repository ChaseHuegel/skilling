# ISSUE-197: Remove static mutable state and test-only hooks from break mechanics

## Context & User Story
- **Goal:** As a maintainer, I want `ChainBreakMechanic`/`AreaHarvestMechanic` to carry no fragile shared static state and no test-only methods in production classes, so the code is thread-safe by construction and production surfaces stay clean.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** Code review of `ChainBreakMechanic` and `AreaHarvestMechanic`.

## Implementation Requirements
- [ ] Replace the plain `static final Set<UUID> CHAINING_PLAYERS` (`HashSet`) with an explicit, defensible concurrency choice (it is only touched on the Bukkit main thread today, but the field is mutable global state shared across all instances and subclasses). Either document why a plain set is safe or use a `ConcurrentHashMap.newKeySet()` with the cost noted.
- [ ] Remove the package-private test hooks (`markChainProcessingForTest`, `clearChainProcessingForTest` on both `ChainBreakMechanic` and `AreaHarvestMechanic`) from production code by making the tests drive the behavior through a supported seam (e.g. an injected collection/supplier or real block mocking).
- [ ] Audit the same pattern in `ModifyTameChanceMechanic#setRandomSource` and `XpBonusMechanic#setClockOverrideNanos` and either keep them under a documented `@VisibleForTesting` convention or replace with the same seam approach.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AreaHarvestMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyTameChanceMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/XpBonusMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{ChainBreak,AreaHarvest}MechanicTest.java`
- **Dependencies:** none.
- **Constraints:** No behavior change to chaining/harvest; the `isChainProcessing` guard used by `SkillEventListener` must keep working exactly as before.

## Verification & Definition of Done
- [ ] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [ ] `./gradlew test` passes, including the relocated/refactored chain/harvest tests.
- [ ] No production class in `mechanic/impl` exposes methods whose only caller is a test.
