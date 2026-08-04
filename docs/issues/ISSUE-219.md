# ISSUE-219: Hot-path logging and low-severity correctness polish

## Context & User Story
- **Goal:** As a server admin, I want the engine to be quiet on the hot path and to correct small correctness/cosmetic issues found in review, without changing gameplay economy.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] **Hot-path INFO log:** `SkillEventListener.grantXp` logs `INFO` for every XP grant (`SkillEventListener.java:498-499`) on the main thread. Move to the existing `plugin.debug(...)` guard.
- [ ] **`time` filter boundary gap:** `Skilling.java:453-457`: `day` matches `time < 12300 || time > 23900` and `night` matches `[13000, 23900]`, leaving dusk (12300-12999) matching neither. Make the boundaries consistent and non-overlapping per the intended semantics.
- [ ] **Sprint/sneak on toggle-off:** `SkillEventListener.java:316-324` dispatches `sprint`/`sneak` on both toggle directions. Gate on `event.isSprinting()`/`event.isSneaking()` (matching the `elytra_glide` handler at `:433-438`).
- [ ] **`player_interact` per-hand double-fire:** `SkillEventListener.java:259-262` dispatches for both main- and off-hand interactions. Consider filtering to the main hand (document if both are intended).
- [ ] **`dispatchToNearby` NPE:** `SkillEventListener.java:245-252` dereferences `location.getWorld()` without a null-location guard; `BrewEvent.getContents().getLocation()` can return null (`:229`). Guard null location/world.
- [ ] **Drop-stack overflow:** `YieldMultiplierMechanic.java:34` can produce stacks above the max size; clamp per stack and drop the remainder as a second stack.
- [ ] **FishingLoot rounding:** `FishingLootMechanic.java:31` `Math.round` inflates fractional multipliers on small stacks; use floor or fractional handling.
- [ ] **Saturation above cap:** `SaturationInjectMechanic.java:22` can set saturation above 20; clamp to the vanilla cap.
- [ ] **Validator-key mismatch:** `Skilling.java:334-337` registers `core:armor_bonus`/`core:knockback_resist` with parameter list `["amount"]` but a validator checking `"duration"` — align the validated key with the declared parameter.
- [ ] **`{skill_name}` lore placeholder:** `SkillMenuBuilder.java:242` binds `skill_name` to `ConstantEvaluator(0)`, so skill-level lore renders `0`; resolve it to the actual display name.
- [ ] **MiniMessage command echo:** `SkillsCommand.java:68-69` deserializes the argument parse error message (which embeds user input, e.g. `SkillParser.java:38`) through MiniMessage; sanitize/escape before rendering.
- [ ] **XP `Long::sum` overflow:** `PlayerProfile.addXp` (`PlayerProfile.java:132-135`) can overflow to negative; clamp/saturate (and consider a max-level XP cap).
- [ ] **`PRAGMA foreign_keys`:** `DatabaseManager.java:48-50` applies the pragma to one pooled connection only; move to Hikari `connectionInitSql` if FKs are ever used.
- [ ] **Empty entity-tag `EnumSet.copyOf`:** `EntityTagResolver.java:114` throws on an empty vanilla entity tag; fall back to `EnumSet.noneOf` (or the `addAll` pattern used by `TagResolver`).

## Technical Specifications & Context
- **Target Files:** as listed per bullet above (`SkillEventListener`, `Skilling`, `SkillManager`, `SkillMenuBuilder`, `SkillsCommand`, `PlayerProfile`, `DatabaseManager`, `EntityTagResolver`, and the named mechanics).
- **Dependencies:** none.
- **Constraints:** No gameplay-economy changes (XP amounts/values unchanged). Greenfield — behavior corrections are allowed. If any item is a deliberate design choice (e.g. per-hand interact), document it instead of changing it.

## Verification & Definition of Done
- [ ] No `INFO` log per XP grant; hot-path logging routed through `debug`.
- [ ] Each correctness/cosmetic fix has a focused unit test where a test target exists.
- [ ] `./gradlew build` and `./gradlew test` pass.
