# ISSUE-281: Unknown item-requirement slot throws on the event path and aborts ability dispatch

## Context & User Story
- **Goal:** As a skill author, I want a typo'd requirement `slot` to be rejected at load, not to break XP granting and all remaining abilities on every matching event. `parseItemRequirement` accepts any `slot` string; `RequirementEngine.resolveSlot()` throws `IllegalArgumentException` at runtime, and the `requirements.check(...)` call in `fireAbilities` sits outside the per-mechanic try/catch, so the exception aborts the whole dispatch (no XP, no other abilities) for that event, repeatedly.
- **Agent Role:** You are an expert backend/QA engineer executing this task.

## Implementation Requirements
- [x] Validate the requirement `slot` value against the supported set in `SkillManager.parseItemRequirement` at load, throwing `IllegalArgumentException` with the offending value.
- [x] As defense in depth, wrap the per-ability `check` call so a runtime failure is logged and treated as a failed ability instead of aborting dispatch.
- [x] Add a unit test: an unknown slot fails at parse time; the event-path guard logs and continues when a check throws.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:488-501` (slot read), `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:234-255` (`resolveSlot` throws at 253), `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:594` (un-guarded check call; the per-mechanic try/catch starts at ~616).
- **Dependencies:** None.
- **Constraints:** Follow the fail-fast-at-load convention from `src/AGENTS.md` section 9. Keep the `RequirementResult` contract; do not return raw booleans.

## Verification & Definition of Done
- [x] Parsing a skill with an invalid `slot` throws `IllegalArgumentException`.
- [x] A unit test simulates a runtime check failure and asserts remaining abilities still fire.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: the known slots (main-hand, off-hand, armor, generic inventory) still resolve exactly as before.
