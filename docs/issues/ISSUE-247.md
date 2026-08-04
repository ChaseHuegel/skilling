# ISSUE-247: ExecuteMechanic does not guarantee the kill and lacks target-state guards

## Context & User Story
- **Goal:** As a skill author, I want `core:execute` to behave predictably: either it reliably executes a low-HP target or it reports failure when it cannot.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `ExecuteMechanic` damages the target with `target.getHealth()` via `target.damage(...)` (`ExecuteMechanic.java:30`), which is armor/absorption-reduced; a heavily armored target at the threshold can survive, yet the mechanic already returned `true` (cost/cooldown consumed). There is also no guard for already-dead, creative, or spectator targets.

## Implementation Requirements
- [x] Make the execute path deterministic: either apply true execution (e.g. `target.setHealth(0)` path that respects the game's death handling) or verify the target actually died and return `false` (no consume) when it survives.
- [x] Guard against already-dead/creative/spectator targets.
- [x] Add unit tests: armored target at threshold, dead target, creative target.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ExecuteMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/mechanic/ExecuteMechanicTest.java`
- **Dependencies:** none.
- **Constraints:** Keep the `threshold` parameter semantics.

## Verification & Definition of Done
- [x] A surviving (armored) target does not consume cost/cooldown.
- [x] Dead/creative/spectator targets no-op safely.
- [x] `./gradlew build` and `./gradlew test` pass.
