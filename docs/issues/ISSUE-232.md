# ISSUE-232: Bound and guard the knockback mechanic (unclamped impulse, radius, no friendly-fire check)

## Context & User Story
- **Goal:** As a server owner, I want `core:knockback` to never launch players/entities arbitrarily far or scan an unbounded area, so a misconfigured ability cannot grief players or tank TPS.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — a level-scaled `force` applies a raw `setVelocity` impulse (bypassing knockback resistance) to any nearby LivingEntity — including other players — and `radius` is unclamped, so the ability can hurl players into lava/void/cliffs and scan a huge region per activation.

## Implementation Requirements
- [x] `KnockbackMechanic.execute` (`KnockbackMechanic.java:36-46`) passes the raw `radius` into `getNearbyLivingEntities(radius)` with no upper bound. Clamp to Bukkit's [0, 32] entity-search cap (mirror `AllyAuraMechanic.clampRadius`).
- [x] Clamp `force` and `vertical` to sane maxima so the resulting impulse cannot launch entities across the map.
- [x] Gate targets through a friendly-fire / PvP-protection check so the mechanic cannot knock other players (mirror `AuraTargetFilter`'s target-selection approach; honor the same `targets`/allies semantics the auras use).
- [x] Add unit tests: radius clamp, force clamp, and that the caster is excluded from targets.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/KnockbackMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/KnockbackMechanicTest.java`
- **Dependencies:** `AuraTargetFilter` / `AllyAuraMechanic.clampRadius` as reference patterns.
- **Constraints:** `MechanicParamValidators.radius` currently only rejects negative constants; enforce the cap at execution time so level-scaled evaluators are also bounded.

## Verification & Definition of Done
- [x] `force`/`vertical`/`radius` are bounded for both constant and level-scaled evaluators.
- [x] Non-player entities are never knocked. (Interpreted as: player entities — the caster and other players — are never knocked; the DoD phrasing is a typo since the mechanic's purpose is knocking non-player hostiles. Both caster and other-player exclusion are tested.)
- [x] `./gradlew build` and `./gradlew test` pass.
