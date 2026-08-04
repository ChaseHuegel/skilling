# ISSUE-250: XpBonusMechanic duration overflow and zero-duration no-op

## Context & User Story
- **Goal:** As a skill author, I want huge or zero `duration` values on `core:xp_bonus` to behave predictably (or be rejected), not silently produce an instant-expiry no-op that still consumes the activation.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low — `(long)(durationSec * 1_000_000_000L)` saturates to `Long.MAX_VALUE` for huge durations (`XpBonusMechanic.java:82`), and `now() + Long.MAX_VALUE` overflows negative so the bonus is immediately expired (still returns `true`, consuming cost). A `duration` of 0 also produces an instant-expiry no-op.

## Implementation Requirements
- [ ] Guard against the overflow (e.g. clamp duration to a sane max) and decide the contract for `duration: 0` — either reject it at load or treat it as "no bonus" without consuming cost/cooldown.
- [ ] Add unit tests for huge and zero durations.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/XpBonusMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/XpBonusMechanicTest.java`
- **Dependencies:** none.
- **Constraints:** Keep `PlayerQuitEvent` cleanup and reload `clearAll` behavior.

## Verification & Definition of Done
- [ ] Huge durations do not overflow to an instant-expiry buff.
- [ ] `duration: 0` follows the chosen contract without wasted activation.
- [ ] `./gradlew build` and `./gradlew test` pass.
