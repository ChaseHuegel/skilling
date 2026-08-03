# ISSUE-205: Gate `ProjectileMechanic` and `TeleportMechanic` on interact action

## Context & User Story
- **Goal:** As a player, I want `core:projectile` and `core:teleport` to trigger only on the intended interaction (right-click), so a left-click or plain block interaction does not consume the ability cost and fire the effect.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `ProjectileMechanic` launches a snowball on any `PlayerInteractEvent` (including left-clicks); `TeleportMechanic` teleports on any interact. Both should gate on the right-click actions (`RIGHT_CLICK_AIR` / `RIGHT_CLICK_BLOCK`), matching `OffhandStrikeMechanic`'s action check.
- [x] Return `false` (no-op) for other actions so the Check/Execute/Consume gating does not spend cost.
- [x] Decide whether the hand matters (main hand vs. off-hand) and document; keep it consistent with `player_interact` trigger semantics.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ProjectileMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/TeleportMechanic.java`
  - `docs/users/capabilities.md`
- **Dependencies:** none.
- **Constraints:** No change to `player_interact` trigger dispatch; only the mechanic's action gating.
- **Note (resolution):** Both mechanics now gate on `RIGHT_CLICK_AIR`/`RIGHT_CLICK_BLOCK` (matching `OffhandStrikeMechanic`); other actions return false. The hand is not restricted — any right-click fires, consistent with the `player_interact` trigger — and this is documented.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New/updated tests assert both mechanics are no-ops on left-click/air interact and fire on right-click.
