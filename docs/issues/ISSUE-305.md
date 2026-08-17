# ISSUE-305: Engine triggers and state filter for the gameplay-coverage batch

## Context & User Story
- **Goal:** As a skill engine user, I want the triggers and a state filter for the new XP sources and abilities planned in REPORT_SKILL-COVERAGE (composting, trading, barter, recipe discovery, smithing, mending, maps, cartography, trial vaults, sniffers, potion throws, and honey harvest), so content tickets can bind XP and abilities to these loops.
- **Agent Role:** You are an expert Java engineer executing this engine ticket. Content tickets reference these triggers.

## Implementation Requirements
- [x] Add the `compost` trigger (`io.papermc.paper.event.block.CompostItemEvent`), dispatched to nearby players of the composter block.
- [x] Add the `trade` trigger (`io.papermc.paper.event.player.PlayerTradeEvent`), dispatched to the trading player.
- [x] Add the `barter` trigger (`org.bukkit.event.entity.PiglinBarterEvent`), dispatched to nearby players of the piglin.
- [x] Add the `recipe_discover` trigger (`org.bukkit.event.player.PlayerRecipeDiscoverEvent`), dispatched to the player.
- [x] Add the `smith` trigger (`org.bukkit.event.inventory.SmithItemEvent`), dispatched to the inventory-view player.
- [x] Add the `mend` trigger (`org.bukkit.event.player.PlayerItemMendEvent`), dispatched to the player.
- [x] Add the `map_fill` trigger (`io.papermc.paper.event.player.PlayerMapFilledEvent`), dispatched to the player.
- [x] Add the `cartography` trigger (`io.papermc.paper.event.player.CartographyItemEvent`), dispatched to the player.
- [x] Add the `vault_change` trigger (`io.papermc.paper.event.block.VaultChangeStateEvent`), dispatched to `getPlayer()` when present.
- [x] Add the `sniffer` trigger (`io.papermc.paper.event.entity.EntityFertilizeEggEvent`), dispatched to `getBreeder()` when a player.
- [x] Add the `potion_splash` trigger covering both `org.bukkit.event.entity.PotionSplashEvent` and `org.bukkit.event.entity.LingeringPotionSplashEvent`, dispatched to the player thrower when the thrower is a player.
- [x] Add the `honey_level` state filter (`below:N`, `above:N`, `exactly:N`) that reads the clicked `Beehive` block state's honey level on `player_interact`, so honey-harvest XP fires only when a hive actually has honey to harvest. Fails closed otherwise. Values validated at load.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/` — new trigger classes
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` — registrations
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` — handlers
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java` — `honey_level` value validation
- **Dependencies:** Existing trigger/state-filter patterns (`chunk_load`, `sleep`, `cause`). `dispatchToNearby` for playerless block/world events.
- **Constraints:** Pillar V (event-driven, O(1)). Player attribution where the event provides it. Fail-fast load validation. Javadoc on all new public API elements.
- **Attribution notes:** `PotionSplashEvent` thrower resolves via the thrown potion's shooter. `smith`/`cartography` resolve via the inventory view. `vault_change`/`sniffer` use nullable player getters and skip when null.

## Verification & Definition of Done
- [x] Trigger-index test entries for every new key + event class.
- [x] Listener dispatch tests for each new trigger (player-attributed and nearby-dispatched paths).
- [x] `honey_level` filter test: per-comparison matching, fail-closed on non-beehive clicks, load-time value validation.
- [x] `./gradlew build` and `./gradlew test` pass.