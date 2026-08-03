# ISSUE-190: Fix pre-existing Projectile test failures (mock eye location)

## Context & User Story
- **Goal:** As a developer, I want `./gradlew test` to pass on `main`, so the project validation gate (root `AGENTS.md`) is green for every issue.
- **Agent Role:** You are an expert QA engineer executing this task.
- **Note:** These 4 failures pre-exist on `main` (verified on a clean checkout at `866f7a8`); they are unrelated to ISSUE-186 but block the build gate.

## Implementation Requirements
- [ ] Fix the `NullPointerException` in `ProjectileReturnMechanic.execute` (`ProjectileReturnMechanic.java:51`) — `player.getEyeLocation()` returns null on the mocked player, so `getDirection()` NPEs. Production players always have an eye location; the fix belongs in the test (stub `getEyeLocation()`), not the mechanic.
- [ ] Affected tests:
  - `ProjectileReturnMechanicTest.tippedArrowPreservesItsEffectsViaItemStack` (line ~129)
  - `ProjectileReturnMechanicTest.enchantedTridentReturnsItsItemAndIsRemoved` (line ~81)
  - `ProjectileReturnMechanicTest.arrowTypesAreReturnedAndRemoved` (line ~100)
  - `ProjectileHitTriggerTest.projectileHitDispatchReturnsThrownTrident` (line ~119)
- [ ] Optionally assert the drop target location is near the player's eye location in one case, so the mock is meaningful rather than a null-return stub.

## Technical Specifications & Context
- **Target Files:**
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ProjectileReturnMechanicTest.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/ProjectileHitTriggerTest.java`
- **Dependencies:** none.
- **Constraints:** Do not weaken the mechanic's null-handling if it is defensive already; verify the mechanic handles a genuinely-null eye location gracefully only if the tests want to cover that edge.

## Verification & Definition of Done
- [ ] `./gradlew :test` passes with zero failures.
- [ ] `./gradlew build` passes.
