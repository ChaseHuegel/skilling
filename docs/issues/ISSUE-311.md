# ISSUE-311: Husbandry — honey harvest and sniffer XP sources

## Context & User Story
- **Goal:** As a server owner, I want harvesting honey from beehives and breeding/using sniffers rewarded in Husbandry, gated so a player can only earn XP from hives that actually have honey.
- **Agent Role:** You are an expert content engineer editing bundled skill YAML. Engine trigger `sniffer` and the `honey_level` state filter come from ISSUE-305.

## Implementation Requirements
- [x] Add a Husbandry XP source for harvesting honey with a glass bottle: `player_interact` filtered on `target: "#minecraft:beehives"`, `tool: "minecraft:glass_bottle"`, and `state: "honey_level:above:4"` (~60).
- [x] Add a Husbandry XP source for harvesting honeycomb with shears: `player_interact` filtered on `target: "#minecraft:beehives"`, `tool: "minecraft:shears"` (single material, not `#minecraft:shears`: no vanilla shears tag exists in the Paper API, so the tag form would fail load-time validation), and `state: "honey_level:above:4"` (~60).
- [x] Add a Husbandry XP source on the `sniffer` trigger (EntityFertilizeEggEvent, the breeder player) (~60).
- [x] Do NOT add a `bucket_fish` trigger or source (too easy to abuse).
- [x] No new abilities are required; the three sources are the deliverable. (Optionally, an Apiarist ability that grants a regen/saturation kick on honey-bottle consumption may be added in a follow-up ticket if desired.)

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/husbandry.yml`.
- **Dependencies:** ISSUE-305 `sniffer` trigger and `honey_level` state filter. `#minecraft:beehives` is a vanilla block tag covering `bee_nest` and `beehive`.
- **Constraints:** The `honey_level:above:4` gate is mandatory. Right-clicking a hive with a bottle or shears fires `player_interact` even when the hive is empty; without the gate it would be freely spammable. Vanilla harvest consumes the hive's honey, so a hive refills only over time, which prevents grinding.

## Verification & Definition of Done
- [x] Bundled-skill auto-sweeps pass.
- [x] The `honey_level` gate is covered by ISSUE-305's filter test.
- [x] `./gradlew build` and `./gradlew test` pass.