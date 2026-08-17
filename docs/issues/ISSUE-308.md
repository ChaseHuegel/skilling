# ISSUE-308: Farming — compost XP and the Compost Bloom ability

## Context & User Story
- **Goal:** As a server owner, I want the composter rewarded in Farming and the L50 ability replaced with Compost Bloom, so the bonemeal economy is part of the farming loop.
- **Agent Role:** You are an expert Java/Paper and content engineer. Engine trigger `compost` comes from ISSUE-305.

## Implementation Requirements
- [ ] Add a Farming XP source on the `compost` trigger (~25), so composting plant matter grants farming XP.
- [ ] Add the `core:area_fertilize` mechanic: on `BlockFertilizeEvent` where the player uses bonemeal, also fertilize matching crops in a configurable radius. Parameters: `radius` (clamped to a safe bound). Event-driven, O(radius), no per-tick work.
- [ ] Register `core:area_fertilize` in `Skilling.registerBuiltinMechanics` with a `radius` load validator.
- [ ] Replace Farming's L50 `nutrient_rich` ability with **Compost Bloom** (L50, `block_break` trigger is no longer used; bind to `BlockFertilizeEvent` via a `bonemeal_use`/`fertilize` hook): `core:area_fertilize` with radius sub-scaling (1 to 3). Lore uses a `{radius}` placeholder.
- [ ] Remove the `nutrient_rich` ability from `src/main/resources/skills/farming.yml`.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AreaFertilizeMechanic.java` (new)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (fertilize dispatch)
  - `src/main/resources/skills/farming.yml`
- **Dependencies:** ISSUE-305 `compost` trigger. The `core:area_fertilize` mechanic hooks `BlockFertilizeEvent` (the vanilla bonemeal event).
- **Constraints:** Radius clamped (Pillar V). Bonemeal remains the required catalyst (Vanilla+ restraint). The replaced L50 slot keeps the 6-tier milestone template intact.

## Verification & Definition of Done
- [ ] `AreaFertilizeMechanicTest`: applies to matching crops in radius, respects the radius clamp, no-op outside radius, non-fertilize event is a no-op.
- [ ] Load validation rejects a negative `radius`.
- [ ] Bundled-skill auto-sweeps pass; `nutrient_rich` is gone and `compost_bloom` parses.
- [ ] `docs/users/capabilities.md` documents `core:area_fertilize`; `docs/users/creating-skills.md` notes the composter source.
- [ ] `./gradlew build` and `./gradlew test` pass.