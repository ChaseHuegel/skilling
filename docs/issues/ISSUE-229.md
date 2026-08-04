# ISSUE-229: Bound the crowd_control mechanic's AoE radius

## Context & User Story
- **Goal:** As a server owner, I want `core:crowd_control` to never scan an unbounded radius so a misconfigured or level-scaled radius cannot tank TPS.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Follow-up:** Discovered while resolving ISSUE-216 (mechanic safety), which bounded the same class of bug for `core:aoe_effect` and `core:field_aura` but left this mechanic out of scope.

## Implementation Requirements
- [ ] `CrowdControlMechanic.execute` (`CrowdControlMechanic.java:38`) passes the raw `radius` parameter into `getNearbyEntities(radius, radius, radius)`, unlike the buff auras which clamp to Bukkit's [0, 32] entity-search cap. Clamp the radius (mirroring `AllyAuraMechanic.clampRadius` / the `clampRadius` added to `AoeEffectMechanic`/`FieldAuraMechanic` in ISSUE-216) before the scan.
- [ ] Add a unit test asserting an oversized radius is clamped (e.g. `getNearbyEntities(32, 32, 32)` is used for a radius of 1000) and that the caster is never affected.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/CrowdControlMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/CrowdControlMechanicTest.java` (new)
- **Dependencies:** none.
- **Constraints:** `AllyAuraMechanic.clampRadius` is the reference pattern for radius bounds (see ISSUE-216). Keep behavior backward-incompatible-tolerant (greenfield).

## Verification & Definition of Done
- [ ] `CrowdControlMechanic` radius is bounded to [0, 32].
- [ ] New test verifies the clamp and that the caster is excluded.
- [ ] `./gradlew build` and `./gradlew test` pass.
