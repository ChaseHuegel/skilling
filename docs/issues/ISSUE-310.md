# ISSUE-310: Alchemy — potion-throw XP sources

## Context & User Story
- **Goal:** As a server owner, I want throwing splash and lingering potions rewarded in Alchemy so brewing potions leads to using them.
- **Agent Role:** You are an expert content engineer editing bundled skill YAML. Engine trigger `potion_splash` comes from ISSUE-305.

## Implementation Requirements
- [x] Add an Alchemy XP source on the `potion_splash` trigger (~40), covering both splash (non-lingering) and lingering potion throws.
- [x] Verify the `potion_splash` trigger fires for a plain splash potion throw (not only lingering). If the trigger does not already cover both, extend ISSUE-305's trigger or add the missing event handler in this ticket.
- [x] No new abilities are added to Alchemy.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/alchemy.yml`.
- **Dependencies:** ISSUE-305 `potion_splash` trigger. Attribution is the player thrower.
- **Constraints:** Flat, low reward. A large-area splash must not multiply into a big grant.

## Verification & Definition of Done
- [x] Dispatch test covers both `PotionSplashEvent` and `LingeringPotionSplashEvent` under `potion_splash`.
- [x] Bundled-skill auto-sweeps pass.
- [x] `./gradlew build` and `./gradlew test` pass.