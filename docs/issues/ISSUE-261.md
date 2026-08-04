# ISSUE-261: Transient attribute modifiers and PENDING_REMOVALS leak on disable/reload

## Context & User Story
- **Goal:** As a server admin, I want `/skills reload` and plugin disable to strip all transient attribute modifiers (Speed/Attack-Speed/Jump/Armor buffs) from online players and clear the pending-removal tracker, so buffs never persist and the static map never grows across reloads.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — stale gameplay state and a static memory leak.

## Implementation Requirements
- [x] Add `AttributeModifierHelper.clearAll()`: cancel every tracked removal task and remove the pending modifiers from online players, then clear `PENDING_REMOVALS`.
- [x] Call it from `Skilling.onDisable` (next to `XpBonusMechanic.clearAll()`, `Skilling.java:669`) and from `LockdownManager.invalidate` (next to `XpBonusMechanic.clearAll()`, `LockdownManager.java:189`).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AttributeModifierHelper.java:31` (`static PENDING_REMOVALS`), `:95-107` (entity-scheduler removal tasks)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:652-686` (`onDisable`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:188-202` (`invalidate`)
  - `src/test/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManagerReloadTest.java`
- **Dependencies:** none.
- **Constraints:** On plugin disable Paper cancels plugin-owned entity-scheduler tasks without running their callbacks, so the currently-scheduled removals never execute; `clearAll` must compensate. Keep per-mechanic behavior intact.

## Verification & Definition of Done
- [x] After reload/disable, online players have no leftover transient attribute modifiers and `PENDING_REMOVALS` is empty.
- [x] `./gradlew build` and `./gradlew test` pass.
