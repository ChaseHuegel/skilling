# ISSUE-265: Cooldown arithmetic overflow silently disables cooldowns

## Context & User Story
- **Goal:** As a skill author, I want an extremely large level-scaled cooldown value to be clamped, never to overflow `currentTimeMillis() + durationMs` negative (making the cooldown instant) or to saturate the ready-message scheduler delay so the "ready" message fires immediately.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — silent failure of a gameplay gate.

## Implementation Requirements
- [ ] Clamp cooldown duration to a sane maximum (e.g. 1 day) before both the `RequirementEngine` expiry arithmetic and the `SkillEventListener` ready-message scheduler delay.
- [ ] Add unit tests for huge cooldown values asserting the cooldown is still enforced.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:139` (`(long)(cdSec * 1000)`), `:247-249` (`System.currentTimeMillis() + durationMs`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:631` (`delayTicks = (long)(cdSec * 20)`; CraftScheduler `currentTick + delay` wraps at saturation)
  - `src/test/java/io/github/chasehuegel/skilling/requirements/RequirementEngineCooldownTest.java`
- **Dependencies:** none.
- **Constraints:** Cooldowns are level-scaled evaluator outputs; the clamp must not change normal-range behavior. A negative overflow currently makes the cooldown instantly expired (never enforced) — clamp, don't reject, unless load-time validation is preferred.

## Verification & Definition of Done
- [ ] Huge cooldowns are enforced for at least the clamped maximum.
- [ ] `./gradlew build` and `./gradlew test` pass.
