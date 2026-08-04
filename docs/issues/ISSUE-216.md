# ISSUE-216: Mechanic safety (teleport, tool-break chains, radius bounds, kill attribution, durability, NPE)

## Context & User Story
- **Goal:** As a player, I want abilities to never self-harm (teleport into the void), never produce free drops with a broken tool, never scan unbounded radii, and never bypass the vanilla damage/durability pipeline.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] **Teleport safety:** `TeleportMechanic` (`TeleportMechanic.java:33-43`) teleports the player up to `range` blocks into the air/void when `getTargetBlockExact` returns null (e.g. looking at the sky). `findSafeLocation` (`:46-61`) only checks two air blocks and never verifies survivable ground. Clamp the target to a location with solid floor within a fall-safe distance, or refuse to teleport when no safe location exists.
- [ ] **Tool-break chain:** `ChainBreakMechanic`/`AreaHarvestMechanic` continue breaking blocks after `ToolDurability.damageOnce` breaks the tool (`ToolDurability.java:102-103`); the amount-0 tool still applies silk-touch/fortune to `breakNaturally`, granting free enchanted drops (`ChainBreakMechanic.java:105-107`). Stop the loop when the tool breaks.
- [ ] **Unbounded radius:** `AoeEffectMechanic` (`AoeEffectMechanic.java:32`) and `FieldAuraMechanic` (`FieldAuraMechanic.java:31`) pass raw config radius into `getNearbyLivingEntities`, unlike `AllyAuraMechanic` which clamps to [0,32]. Clamp radius and/or add a load-time upper bound.
- [ ] **Kill attribution:** `ExecuteMechanic` (`ExecuteMechanic.java:28`) uses `target.setHealth(0)`, bypassing the damage pipeline (no `EntityDamageByEntityEvent`, lost killer attribution/vanilla XP). Route through `target.damage(target.getHealth(), player)` or an attributed kill.
- [ ] **Offhand durability:** `OffhandStrikeMechanic` (`OffhandStrikeMechanic.java:77-80`) increments damage directly, bypassing `PlayerItemDamageEvent` (Unbreaking not rolled, other plugins can't veto) and never breaks the item at max durability. Route through `PlayerItemDamageEvent` and break like `ToolDurability`.
- [ ] **RequirementEngine NPE:** `RequirementEngine.resolveMaterialSet` (`RequirementEngine.java:196`) NPEs on a null item `tag`. Reject a missing `tag` at load in `SkillManager.parseItemRequirement` (`SkillManager.java:381-389`) or null-guard the resolver.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/TeleportMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{ChainBreak,AreaHarvest}Mechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ToolDurability.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{AoeEffect,FieldAura,AllyAura}Mechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ExecuteMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/OffhandStrikeMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java` + `SkillManager.java`
  - Tests: `TeleportMechanicTest`, `ChainBreakMechanicTest`, `AreaHarvestMechanicTest`, `AoeEffectMechanicTest`, `OffhandStrikeMechanicTest`, `RequirementEngineTest`
- **Dependencies:** none.
- **Constraints:** Keep behavior backward-incompatible-tolerant (greenfield). The `AllyAuraMechanic.clampRadius` pattern is the reference for radius bounds.

## Verification & Definition of Done
- [ ] Teleport never places the player in the void or an unsurvivable fall.
- [ ] Chain/area harvest stops at tool break; no drops from a broken tool.
- [ ] AOE/field aura radii bounded; oversized config rejected or clamped at load.
- [ ] ExecuteMechanic kills are attributed through the damage pipeline.
- [ ] Offhand strike rolls `PlayerItemDamageEvent` and breaks the item at max durability.
- [ ] A missing item `tag` fails fast or is null-safe.
- [ ] `./gradlew build` and `./gradlew test` pass.
