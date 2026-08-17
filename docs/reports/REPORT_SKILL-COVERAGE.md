# REPORT: Minecraft Gameplay Coverage Audit and Skill Proposals

- **Owning ticket:** [ISSUE-304](../issues/ISSUE-304.md)
- **Date:** August 2026
- **Status:** Research only. No production code changed.

## Purpose

This report audits how far the bundled skills cover Minecraft's gameplay loops,
then proposes XP sources and abilities for the uncovered and under-utilized
aspects. Every proposal aims for fun, simplicity, value for under-used
features, and a tight Vanilla+ feel.

All proposals follow the engine's design pillars from
`docs/dev/SKILL-DESIGN-FRAMEWORK.md`:

- Event-driven and O(1). No per-tick scanning.
- Player-attributed where possible.
- Vanilla+ restraint. Enhance vanilla, never replace it.
- Dual-layer scaling and the 6-tier milestone template.
- Clean unplug. No world corruption, no NMS tile-entity hacks.

## How to Read This Report

Each proposal has four parts:

- **Theme.** What vanilla loop it covers.
- **XP sources.** The triggers, filters, and rewards. "New trigger" means we
  must add a trigger to the engine.
- **Abilities.** Ideas at the 6 milestone tiers. Most reuse existing mechanics.
  A few need a minimal new mechanic, which is named and justified.
- **Vanilla+ and risks.** Where the idea could drift off-theme, get abused, or
  lose player attribution.

A full build list of engine additions is at the end.

---

## 1. Coverage Audit

### 1.1 What the bundle already covers

| Gameplay area | Skill | XP loop |
|---|---|---|
| Mining / ores | Mining | break stone and ores |
| Wood | Woodcutting, Carpentry | break logs, craft wooden products |
| Digging | Excavation | break dirt/gravel/sand, craft shovels |
| Crops | Farming, Herbalism | break crops and herbs, craft crop products |
| Fishing | Fishing | the fishing trigger |
| Food | Cooking, Survival | furnace/consume, campfire, eat |
| Combat (melee) | Six weapon skills | damage, kill, take damage |
| Combat (ranged) | Archery, Throwing | shoot/launch, damage with ammo |
| Armor / defense | Four armor skills, Shields, Unarmored | take damage armored, craft armor |
| Alchemy | Alchemy | brew potions, consume potions |
| Enchanting | Enchanting | enchant, collect XP, repair |
| Smithing / crafting | Smithing, Masonry, Tailoring | furnace, craft, repair |
| Building | Building | place blocks, craft |
| Husbandry / animals | Husbandry | breed, shear, tame |
| Riding | Riding | ride, tame, mounted combat |
| Acrobatics / mobility | Acrobatics | fall damage, elytra, ender pearl |
| Religion / totems | Piety | bury bones, undead, resurrect, cure |
| Music | Bard | instruments, jukebox |
| Camping / survival | Survival | environmental damage, campfires, swim, explore, sleep |

### 1.2 Uncovered and under-utilized loops

| Loop | Covered? | Notes |
|---|---|---|
| Archaeology (brush, suspicious blocks, pottery) | No | No skill, no trigger |
| Redstone contraptions | No | No skill, no trigger |
| Composting / bonemeal economy | No | Composter ignored |
| Villager trading | No | Piety covers curing only |
| Piglin barter | No | No skill |
| Beekeeping | Partial | Husbandry breeds bees, no honey loop |
| Sniffers (torchflower/pitcher seeds) | No | No trigger |
| Axolotl and rare aquatic capture | No | No trigger |
| Trial chambers and vaults | No | 1.21 content unused |
| Armor trims / smithing table | Partial | Smithing ignores the smithing table |
| Maps / cartography | No | Under-used |
| Looting structure chests | No | Under-used |
| Decoration (armor stands, frames, signs) | No | Building ignores these |
| Music discs | Partial | Bard crafts/places jukebox, no disc loop |
| Bells | No | Village feature unused |
| Sculk / deep dark | No | Under-used |
| Fireworks | No | Under-used |
| Mace / wind charge | Partial | Mace in heavy weapons tag |
| Riptide | No | Acrobatics covers elytra only |
| Mending | No | Enchanting adjacent, unused |
| Splash/lingering potion use | No | Alchemy brews but never rewards use |
| Recipe discovery | No | No trigger |
| Recipes gated by progression | No | `core:unlock_recipe` unused in bundle |

---

## 2. Cross-cutting Engine Additions

These triggers and mechanics serve several proposals. The build list at the end
prioritizes them.

### 2.1 Player-attributed triggers

| Key | Event | Attribution |
|---|---|---|
| `swim` | `EntityToggleSwimEvent` | Player |
| `trade` | `io.papermc.paper.event.player.PlayerTradeEvent` | Player |
| `barter` | `org.bukkit.event.entity.PiglinBarterEvent` | Nearby players (piglin) |
| `recipe_discover` | `org.bukkit.event.player.PlayerRecipeDiscoverEvent` | Player |
| `smith` | `org.bukkit.event.inventory.SmithItemEvent` | Player |
| `map_fill` | `io.papermc.paper.event.player.PlayerMapFilledEvent` | Player |
| `cartography` | `io.papermc.paper.event.player.CartographyItemEvent` | Player |
| `mend` | `org.bukkit.event.player.PlayerItemMendEvent` | Player |
| `vault_change` | `io.papermc.paper.event.block.VaultChangeStateEvent` | Player (nullable) |
| `sniffer` | `io.papermc.paper.event.entity.EntityFertilizeEggEvent` | Breeder (nullable) |
| `bucket_fish` | `org.bukkit.event.player.PlayerBucketFishEvent` | Player |
| `potion_splash` | `org.bukkit.event.entity.LingeringPotionSplashEvent` | Player thrower |
| `armor_stand` | `org.bukkit.event.player.PlayerArmorStandManipulateEvent` | Player |

### 2.2 Nearby-dispatched triggers

These are world or block events without an owning player. The engine routes
them to nearby players, the same way it already does for
`chunk_load` and `brew_potion`.

| Key | Event | Note |
|---|---|---|
| `campfire_cook` | `org.bukkit.event.block.CampfireStartEvent` | Replaces the Survival player_interact hack |
| `compost` | `io.papermc.paper.event.block.CompostItemEvent` | Composter gets one item |
| `craft_crafter` | `org.bukkit.event.block.CrafterCraftEvent` | The 1.21 Crafter |
| `note_play` | `org.bukkit.event.block.NotePlayEvent` | Note block |
| `piston` | `org.bukkit.event.block.BlockPistonExtendEvent` / `RetractEvent` | Contraptions |
| `bell` | `org.bukkit.event.block.BellRingEvent` | Village bell |
| `loot` | `org.bukkit.event.world.LootGenerateEvent` | Structure chests |
| `redstone` | `org.bukkit.event.block.BlockRedstoneEvent` | High spam risk, see 3.2 |

### 2.3 New mechanics

| Key | Purpose | Hook |
|---|---|---|
| `core:area_fertilize` | AOE bonemeal around the fertilized block | `BlockFertilizeEvent` |
| `core:barter_luck` | Improve or add to piglin barter outcome | `PiglinBarterEvent` (editable outcome list) |
| `core:cleanse` | Remove negative effects from the player | `EntityPotionEffectEvent` or a self-target |

---

## 3. Proposals

### 3.1 Archaeology: brush, suspicious blocks, pottery

**Theme.** Brushing suspicious sand and gravel, crafting decorated pots, and
looting trial-chamber vaults. This is the biggest fully-unused gameplay system
in the bundle.

**Cleanest fit.** Archaeology is a digging activity. The strongest Vanilla+
answer is to fold it into **Excavation**, which already owns
dirt/gravel/sand. Suspicious sand and suspicious gravel are literally
excavation blocks. This avoids a thin six-ability skill and strengthens the
existing one.

**If we want a themed identity.** A standalone Archaeology skill works if we
pair it with the trial-chamber and vault loop to fill six tiers.

**XP sources (both options).**

```yaml
# Excavation additions (folded option)
xp_sources:
  - trigger: "player_interact"                  # brush suspicious blocks
    filters:
      - target: "minecraft:suspicious_sand"
      - tool: "minecraft:brush"
    reward: { constant: 60.0 }
  - trigger: "block_break"
    filters: [ { target: "#c:suspicious_blocks", state: "player_placed:false" } ]
    reward: { constant: 40.0 }
  - trigger: "craft_item"
    filters: [ { target: "minecraft:decorated_pot" } ]
    reward: { constant: 60.0 }
```

Add one custom tag `#c:suspicious_blocks` for `suspicious_sand` and
`suspicious_gravel`.

**Abilities (folded option, top two for Excavation):**

- **Steady Hand (L15).** `core:durability_save` on the held `minecraft:brush`
  while brushing, 20 to 70 percent. Brushing is an `item_damage` event, so the
  existing mechanic applies.
- **Relic Hunter (L50).** `core:xp_bonus` on the brush interact, granting a
  short excavation XP boost after dusting a suspicious block.

**Abilities (standalone Archaeology, six tiers):**

| Tier | Ability | Mechanic / idea |
|---|---|---|
| 1 | Steady Hand | brush durability save (existing) |
| 15 | Studied Relic | brush interact gives a short XP boost (existing) |
| 25 | Active: Sweep | Sneak + Right-Click with a brush to brush in a small arc (new small mechanic) |
| 50 | Vault Seeker | opening a `vault_change` gives Absorption (existing field aura) |
| 75 | Pottery | crafting `decorated_pot` gives craft-output or XP bonus (existing) |
| 100 | Relic Hunter | scavenging suspicious blocks grants a luck-style absorption/regen kick |

**Vanilla+ and risks.** Brushing has no per-tick cost. The brush-break
durability perk is a friction reducer, not a replacement. Trial-vault XP
rewards the 1.21 exploration loop. Keep `player_placed:false` on the suspicious
block so server owners cannot place and re-mine for XP.

**Recommendation.** Fold the brush and pottery XP into Excavation. Add the
standalone skill only if the trial chamber and vault theme is wanted as its own
identity.

---

### 3.2 Redstone and contraptions

**Theme.** Redstone dust, repeaters, pistons, note blocks, dispensers, hoppers,
the Crafter, and powered contraptions.

**Honest constraint.** Most redstone events carry no player attribution and can
fire in tight loops. An observer clock powers a chain of `BlockRedstoneEvent`
per tick. Granting XP or effects on raw redstone current is an anti-grind and
performance trap. The report recommends a crafting-and-placement-first design:
reward building contraptions, not powering them.

**Cleanest fit.** Redstone is a construction activity. Fold it into
**Building** as XP sources, with optional milestone recipe unlocks. A
standalone Tinkering skill is viable but thin on distinct abilities.

**XP sources (folded into Building).**

```yaml
# custom tag #c:redstone_components: dust, torch, repeater, comparator, piston,
# sticky_piston, observer, hopper, dispenser, dropper, redstone_lamp, lever,
# daylight_detector, target, note_block, crafter, tripwire_hook, rails
  - trigger: "craft_item"
    filters: [ { target: "#c:redstone_components" } ]
    reward: { constant: 60.0 }
  - trigger: "block_place"
    filters: [ { target: "#c:redstone_components" } ]
    reward: { constant: 30.0 }
  - trigger: "note_play"            # new trigger, nearby dispatch
    reward: { constant: 25.0 }
  - trigger: "craft_crafter"        # new trigger, nearby dispatch
    reward: { constant: 40.0 }
```

Gating note: `note_play` and `craft_crafter` can both loop. A note block
replaying a melody fires many events. A hopper-fed Crafter crafts on a clock.
Apply a flat low reward and rely on the engine's per-ability cooldown for
abilities, not for XP. If a loop proves grindy, drop those two XP sources and
keep only craft and place.

**Abilities (folded into Building):**

- **Redstone Sense (L15).** `core:haste_effect` after placing a redstone
  component. Rewards sustained building.
- **Overclock (L50).** `core:xp_bonus` while tinkering, an efficiency spike.
- **Milestone unlock.** Use `core:unlock_recipe` at L25 and L75 to gate
  data-pack redstone devices behind levels. This is the binary-unlock exception.

**Standalone Tinkering skill.** Same XP loop. Six abilities are hard to
differentiate, so the report does not recommend it unless the milestone recipe
gating carries most of the identity.

**Vanilla+ and risks.** Console-style redstone perks that speed up or "auto"
contraptions would need NMS or per-tick jobs. The report avoids both. The
folded design keeps redstone Vanilla while giving the builder XP and a couple
of passive perks.

**Recommendation.** Fold redstone crafting and placement into Building. Do not
build perks on raw redstone current.

---

### 3.3 Composter and bonemeal: Farming

**Theme.** The composter turns plant matter into bonemeal. Bonemeal use is a
strong farming action. Both are under-used as XP and as an ability hook.

**XP source (Farming):**

```yaml
  - trigger: "compost"              # new trigger; starts when an item is composted
    filters: [ { state: "player_placed:false" } ]
    reward: { constant: 25.0 }
```

The compost trigger dispatches to nearby players, so it works in a farm
run by anyone present.

**New mechanic: `core:area_fertilize`.** When a player uses bonemeal on a crop
(`BlockFertilizeEvent`), also fertilize matching crops in a small radius. This
is event-driven, O(radius), and vanilla-bonemeal only. It enhances an existing
item, never replaces it.

**Abilities (Farming):**

- **Compost Bloom (L50).** `core:area_fertilize`, radius sub-scales from 1 to
  3. The composter-fed bonemeal spreads farther.
- **Black Thumb (L25).** `core:haste_effect` or `core:saturation_inject` after
  filling a composter, a light ease-of-life bonus.

**Vanilla+ and risks.** Radius is clamped (Pillar V) and the fertilize event
still respects vanilla rules. Composter XP is flat and low so a large compost
farm does not flood a player's skill.

**Recommendation.** Add the farming compost source and the
`core:area_fertilize` ability. This is high value and low effort.

---

### 3.4 Trading: villagers and piglins

**Theme.** Villager trading, the biggest social-economy system in the game, is
entirely unused. Piglin barter is a smaller second loop.

**New skill: Trade (or Merchant).**

**XP sources:**

```yaml
  - trigger: "trade"                # new trigger, PlayerTradeEvent
    reward: { constant: 60.0 }
  - trigger: "barter"               # new trigger, PiglinBarterEvent
    reward: { constant: 60.0 }
  - trigger: "cure_villager"        # shared with Piety; multi-skill XP is normal
    reward: { constant: 60.0 }
```

**Abilities (six tiers):**

| Tier | Ability | Mechanic / idea |
|---|---|---|
| 1 | Merchant's Eye | `trade` grants a short `xp_bonus` (existing) |
| 15 | Barter Charm | `core:barter_luck`, improve the piglin outcome (new mechanic, outcome list is editable) |
| 25 | Active: Haggling | replicate the vanilla "Hero of the Village" discount feel with a short consumable buff (existing) |
| 50 | Currency | trading grants a craft/emerald synergy (existing) |
| 75 | Cured Ally | curing a villager grants a trading `xp_bonus` (existing) |
| 100 | Tycoon | trade grants Absorption for the session (existing field aura) |

**Vanilla+ and risks.** Direct villager price control is limited on the API.
The report favors passive "luck" and trade-count rewards over price hacks.
Piglin barter is an editable outcome list, so `core:barter_luck` is a real,
safe mechanic.

**Recommendation.** Add Trade as a new skill. It gives the economy system a
home and has a strong, distinct identity.

---

### 3.5 Exploration: maps, loot, structures, trial chambers

**Theme.** Travel, cartography, scavenging, and the deep dark. Part of this
loop already lives in Survival (chunk XP and sleep). This proposal gives the
travel theme a dedicated home if the overlap is acceptable.

**XP sources (new skill: Exploration):**

```yaml
  - trigger: "map_fill"             # new trigger, PlayerMapFilledEvent
    reward: { constant: 40.0 }
  - trigger: "cartography"          # new trigger, CartographyItemEvent
    reward: { constant: 60.0 }
  - trigger: "loot"                 # new trigger, nearby dispatch
    reward: { constant: 60.0 }
  - trigger: "vault_change"         # new trigger
    reward: { constant: 80.0 }
  - trigger: "chunk_load"           # already in Survival; a biome-filtered copy
    filters: [ { state: "biome:minecraft:badlands" } ]
    reward: { constant: 40.0 }
```

**Abilities (six tiers):**

| Tier | Ability | Mechanic / idea |
|---|---|---|
| 1 | Pathfinder | exploring a new chunk grants a short `speed_bonus` (existing) |
| 15 | Cartographer | `cartography` grants `xp_bonus` (existing) |
| 25 | Scavenger | `loot` grants a craft/consume ease bonus (existing) |
| 50 | Trailblazer | `map_fill` grants `speed_bonus` (existing) |
| 75 | Deep Dark | `sculk` or `loot` under a `dimension:nether/end` filter gives a survival buff (existing) |
| 100 | Vault Raider | `vault_change` grants Absorption, like Survival's Well Rested (existing) |

**Overlap decision.** Survival already owns `chunk_load` and `sleep`. If
Exploration is a skill, decide who owns chunk XP. Multi-skill XP on the same
action is normal in the bundle, so both can share it. The report suggests each
skill keeps its own flavor: Survival for camping, Exploration for maps and
loot.

**Vanilla+ and risks.** `loot` dispatches to nearby players, so two players
looting the same chest both gain XP. That is acceptable and thematic for a
group adventure. A `biome`-filtered chunk source rewards traveling to new
biomes, but a player can also traverse a thin strip of new biomes repeatedly.
Keep the reward flat and low.

**Recommendation.** Add Exploration only if the map, cartography, and vault
loop is worth a dedicated identity. Otherwise fold maps and loot into Survival
and keep Archaeology in Excavation.

---

### 3.6 Beekeeping, Sniffers, and rare aquatic: Husbandry

**Theme.** The animal skill already breeds bees. It misses the honey, the
sniffer, and the axolotl loops.

**XP sources (Husbandry):**

```yaml
  - trigger: "player_interact"
    filters:
      - target: "#minecraft:beehives"
      - tool: "minecraft:glass_bottle"   # or shears for honeycomb
    reward: { constant: 60.0 }
  - trigger: "sniffer"              # new trigger, EntityFertilizeEggEvent
    reward: { constant: 60.0 }
  - trigger: "bucket_fish"          # new trigger, PlayerBucketFishEvent
    filters: [ { target_type: "minecraft:axolotl" } ]
    reward: { constant: 40.0 }
```

**Abilities:**

- **Apiarist (L25).** consuming a `minecraft:honey_bottle` grants a short
  `saturation` or regen kick (existing). Honey is a vanilla survival item that
  Cooking does not own.
- **Green Thumb, Wild (L50).** breeding sniffers feeds the farming loop. Give a
  small `xp_bonus` after a sniffer buries seeds (existing).

**Vanilla+ and risks.** Honey extraction is a real, infrequent action, so it is
not grindy. Sniffer breeding needs copper ingots as a cost, which keeps the
economy intact.

**Recommendation.** Fold these into Husbandry. They are low-effort XP additions
plus one or two ease perks.

---

### 3.7 Smithing table and armor trims: Smithing

**Theme.** The smithing table re-forges gear and applies armor trims. Trims
are cosmetic and under-used; using the table is under-rewarded.

**XP source (Smithing):**

```yaml
  - trigger: "smith"                # new trigger, SmithItemEvent
    reward: { constant: 80.0 }
```

**Abilities:**

- **Master Smith (L50).** after smithing, grant a short `haste_effect` or
  `saturation_inject` ease bonus (existing).
- **Trimmed Armor (L75).** a trim-enhanced smithing event grants Absorption
  (existing field aura).

**Vanilla+ and risks.** Do not make trims grant combat stats. Trims are
cosmetic; mechanical bonuses would change vanilla expectations. Keep trim
benefits to XP and ease perks.

**Recommendation.** Add the `smith` XP source. This rewards an under-used
vanilla table at near-zero effort.

---

### 3.8 Potion use: Alchemy

**Theme.** Alchemy brews potions but never rewards using them. Splash and
lingering potions are a big part of the combat-potion toolkit.

**XP source (Alchemy):**

```yaml
  - trigger: "potion_splash"        # new trigger, LingeringPotionSplashEvent
    reward: { constant: 40.0 }
```

**Abilities:**

- **Master Alchemist (L75).** after a splash, grant a short self `haste` or
  `saturation_inject` (existing).

**Vanilla+ and risks.** A splash potion targets a few entities, so the reward
must stay flat and low. Do not double-count a large AoE hit.

**Recommendation.** Add the `potion_splash` XP source and one ease perk.

---

### 3.9 Decoration: Building

**Theme.** Armor stands, item frames, signs, and flower pots are player-built
decoration loops that Building ignores.

**XP sources (Building):**

```yaml
  - trigger: "armor_stand"          # new trigger, PlayerArmorStandManipulateEvent
    reward: { constant: 30.0 }
  - trigger: "sign_edit"            # new trigger, SignChangeEvent
    reward: { constant: 40.0 }
```

**Abilities:**

- **Display (L25).** placing or editing an armor stand grants a short
  `haste_effect` (existing).
- **Handiwork (L50).** decoration crafting grants `modify_craft_output` (existing).

**Vanilla+ and risks.** Armor-stand manipulation can be spammy when a player
toggles parts. Apply a cooldown on the ability or a low flat XP reward.

**Recommendation.** Add these as low-priority XP sources. They round out
Building's decorative side without adding a new skill.

---

### 3.10 Mending, bells, music discs, and fireworks: small adds

**Mending (Enchanting).**

```yaml
  - trigger: "mend"                 # new trigger, PlayerItemMendEvent
    reward: { constant: 30.0 }
```

**Bells (Bard or Building).**

```yaml
  - trigger: "bell"                 # new trigger, BellRingEvent
    reward: { constant: 25.0 }
```

**Music discs (Bard).** insert a disc into a jukebox with `player_interact`
filtered on `tool` disc materials; grant bard XP.

**Fireworks.** keep light. Crafting fireworks feeds an existing crafting skill.
Do not build elaborate firework abilities.

---

## 4. Folding and Blurring Decisions

The bundle is already well-differentiated. The report recommends folding in
these places and not folding these:

**Recommended folds.**

- Archaeology into Excavation (section 3.1).
- Redstone into Building (section 3.2).
- Barter into Trade (section 3.4).
- Beekeeping, sniffers, axolotl into Husbandry (section 3.6).
- Composter into Farming (section 3.3).

**Keep separate.**

- The six melee and four armor skills. They are distinct playstyles.
- Piety from Trade. Piety owns curing and totems. Trade owns buying and
  bartering. They share the `cure_villager` XP source, not their identities.
- Acrobatics from Survival. Acrobatics is movement; Survival is camping and
  the environment.

**Overlap to decide.**

- `chunk_load` lives in both Survival and Exploration (section 3.5). Share it
  or pick one owner.

---

## 5. Recipe Gating and the Discovery Loop

**The loop.** `knowing a recipe` is a vanilla progression moment. It is unused
by every bundled skill.

**New trigger: `recipe_discover`** (`PlayerRecipeDiscoverEvent`). A
crafting or new Discovery skill can grant XP when a player reveals a recipe in
the recipe book.

**Recommendation: use `core:unlock_recipe` widely.** The mechanic already
unlocks recipes on a `level_up` milestone and reconciles on join and reload.
Use it to gate meaningful recipes at milestone tiers. For data-pack recipes,
the server owner provides the recipe and the bundle unlocks it. This is the
cleanest "progression-into-crafting" tool in the engine.

**Vanilla+ and risks.** Gating vanilla basics hurts the new-player experience.
Gate only late-game or high-value recipes. Keep the early loop free. The
"discovery" trigger is a gentle XP source, not a gate.

---

## 6. Priority Roadmap

**P0: small, high value, low effort.**

- Add `swim` and `campfire_cook` triggers and switch Survival to them. This
  fixes the current `sprint+is_in_water` hack and the interact-based campfire
  hook.
- Add `smith` XP to Smithing.
- Add `compost` XP and the `core:area_fertilize` ability to Farming.
- Add `recipe_discover` and use `core:unlock_recipe` for a few milestone
  gates.
- Add `potion_splash` XP to Alchemy.

**P1: new skills with strong identity.**

- Trade / Merchant, with the `barter_luck` mechanic.
- Exploration, only if the map and vault loop justifies the overlap.
- Archaeology only if the trial-chamber and pottery theme is chosen over the
  Excavation fold.

**P2: rounding out existing skills.**

- Redstone crafting into Building.
- Beekeeping, sniffers, and axolotl into Husbandry.
- Decoration into Building.
- Mending, bells, and discs as small XP adds.

---

## 7. Open Questions for Design

1. Do we want a standalone Archaeology identity, or fold it into Excavation?
2. Do we want a standalone Exploration skill, or fold maps and loot into
   Survival?
3. Who owns `chunk_load` XP: Survival, Exploration, or both?
4. How aggressively should `core:unlock_recipe` gate vanilla recipes? The
   report recommends a light touch.
5. Which redstone XP sources are safe from observer-clock grinding? The report
   recommends craft and place only, and to drop note/crafter sources if they
   prove grindy.

---

## Appendix A. Engine Build List

**Triggers to add.**

`swim`, `campfire_cook`, `trade`, `barter`, `recipe_discover`, `smith`,
`map_fill`, `cartography`, `mend`, `vault_change`, `sniffer`, `bucket_fish`,
`potion_splash`, `armor_stand`, `sign_edit`, `compost`, `craft_crafter`,
`note_play`, `piston`, `bell`, `loot`, `redstone`.

**Mechanics to add.**

`core:area_fertilize`, `core:barter_luck`, `core:cleanse`.

**Tags to add.**

`#c:suspicious_blocks`, `#c:redstone_components`, `#c:decorative_blocks`
(reuse or extend `#c:wooden_products` and `#c:stone_products`), disc
materials for Bard.

## Appendix B. Sources

- Paper API event inventory, current release, verified for
  `CampfireStartEvent`, `CrafterCraftEvent`, `CompostItemEvent`,
  `VaultChangeStateEvent`, `PlayerTradeEvent`, `EntityFertilizeEggEvent`,
  `PlayerRecipeDiscoverEvent`, and the nearby-dispatched block events.
- `docs/users/capabilities.md` and `docs/dev/SKILL-DESIGN-FRAMEWORK.md` for the
  current engine surface and design pillars.
