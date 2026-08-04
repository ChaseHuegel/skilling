# ISSUE-233: OffhandStrikeMechanic can damage any living entity, including other players

## Context & User Story
- **Goal:** As a server owner, I want `core:offhand_strike` to respect PvP/friendly-fire rules and bounded reach/multiplier so a cheap right-click ability cannot damage arbitrary players or entities at range.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — the strike raycasts with an unclamped `reach` and applies an unclamped `multiplier` to whatever `LivingEntity` the player is looking at (`OffhandStrikeMechanic.java:71-75`), including other players, with no PvP protection, region check, or reach cap.

## Implementation Requirements
- [ ] Bound `reach` to a sane maximum (e.g. the vanilla attack reach) so the raycast cannot hit targets from across a room.
- [ ] Bound `multiplier` so the strike cannot deliver arbitrarily large damage.
- [ ] Add a friendly-fire / PvP-protection check before `livingTarget.damage(dmg, player)` so non-combat/other-player targets are not struck (mirror the target-gating approach used by `AuraTargetFilter` and ISSUE-232).
- [ ] Add unit tests covering reach cap, multiplier cap, and the PvP guard.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/OffhandStrikeMechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/OffhandStrikeMechanicTest.java`
- **Dependencies:** none.
- **Constraints:** Keep the durability-consumption and PlayerInteractEvent gating as-is; greenfield.

## Verification & Definition of Done
- [ ] `reach` and `multiplier` are bounded for constant and level-scaled evaluators.
- [ ] The strike never damages another player (or is gated by the server's PvP rules).
- [ ] `./gradlew build` and `./gradlew test` pass.
