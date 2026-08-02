# ISSUE-126: Fix `TeleportMechanic` consuming cooldown/items when the teleport fails

**Status:** Open
**Type:** Bug
**Severity:** Medium (Check-Execute-Consume contract violated)

---

## Context & User Story

- **Goal:** As a player, I want to keep my cooldown and items when a teleport ability fails to find a safe location.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Return `false` (no-op, no consume) when `findSafeLocation` returns null or the player is not moved
- [ ] Return `true` only when the teleport actually occurs
- [ ] Add a unit test covering the no-safe-location path

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/TeleportMechanic.java:37-45`
- **Dependencies:** The Check-Execute-Consume loop (`SkillEventListener.java:439-515`, see ISSUE-116).
- **Constraints:** Keep the YAML contract (`distance`, etc.) unchanged.

### Root Cause

When `findSafeLocation` returns null, the mechanic still `return true`, so `fireAbilities` proceeds to `requirementEngine.consume(...)` — cooldown/items are consumed for a teleport that never happened.

### Proposed Fix

Return `false` from the failed-location path so the engine skips consume and feedback.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: no safe location → `execute` returns false
- [ ] Unit test: successful teleport → `execute` returns true
