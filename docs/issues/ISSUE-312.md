# ISSUE-312: New bundled skill — Trade (draft design, refinement required)

## Context & User Story
- **Goal:** As a server owner, I want a Trade skill that rewards villager trading and piglin barter, with abilities that speed up trade tiering, boost emerald income, re-roll villager offers, and summon traders. The L1 passive and L100 capstone are replaced with stronger, non-XP-buff designs. Piety keeps exclusive ownership of `cure_villager`.
- **Agent Role:** You are an expert game designer and Java/Paper engineer. The ability set in this ticket is a draft; confirm the L1 and L100 designs and final numbers with the design owner before locking the YAML.

## Implementation Requirements
- [x] Create `src/main/resources/skills/trade.yml` (id `trade`, icon `minecraft:emerald`, color `GREEN`, 6-tier milestone template, polynomial curve) and add it to the `bundledSkills` array and the default `gui.yml`.
- [x] XP sources:
  - `trade` trigger (`PlayerTradeEvent`) (~60)
  - `barter` trigger (`PiglinBarterEvent`) (~60)
  - Do NOT use `cure_villager`; Piety owns it.
- [x] Draft abilities (confirm with design owner):
  - L1 **Emerald Eye** (passive): every trade has a chance to pay a bonus emerald, via the new `core:trade_bonus` mechanic (rolls a chance and grants an emerald directly to the player's inventory). Linear chance sub-scaling.
  - L15 **Word of Mouth** (passive): trading grants the traded villager bonus experience via the new `core:villager_xp` mechanic, so its trades tier up faster. Sub-scaling amount.
  - L25 **Recruiter** (active): Sneak + Right-Click with an emerald cost summons a jobless villager via the new `core:summon_villager` mechanic. Cooldown.
  - L50 **Golden Tongue** (passive): piglin barter outcomes improve via the new `core:barter_luck` mechanic (the outcome list is editable). Sub-scaling.
  - L75 **Fresh Stock** (active): Sneak + Right-Click a villager re-rolls its offers by cycling its profession via the new `core:reroll_trades` mechanic, an alternative to breaking and replacing its workstation. Emerald cost and cooldown.
  - L100 **Traveling Merchant** (active): summons a wandering trader via the new `core:summon_wandering_trader` mechanic. High emerald cost and long cooldown.
- [x] Register the mechanics `core:trade_bonus`, `core:villager_xp`, `core:summon_villager`, `core:barter_luck`, `core:reroll_trades`, `core:summon_wandering_trader` in `Skilling.registerBuiltinMechanics` with load validators where applicable.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/resources/skills/trade.yml` (new)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (bundledSkills + mechanics registration)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/` (new mechanic classes)
  - `src/main/resources/gui.yml`
- **Dependencies:** ISSUE-305 `trade` and `barter` triggers.
- **API feasibility (verify during implementation):**
  - `Villager#getVillagerExperience()` / `setVillagerExperience(int)` for faster tiering.
  - Profession cycling (`setProfession(Profession.NONE)` then restore) to re-roll offers.
  - `world.spawn(..., Villager.class)` / `WanderingTrader.class` for summons. Wandering traders keep vanilla despawn behavior.
  - `PiglinBarterEvent#getOutcome()` is an editable list for `core:barter_luck`.
  - `core:trade_bonus` grants emeralds directly to the player inventory rather than mutating the merchant recipe result.
- **Constraints:** No `cure_villager` anywhere in the skill. Summons are transient entities (clean unplug). Cooldowns and costs gate the actives. Abilities follow the 6-step audit checklist and the `&7Costs` / `&8Requires` lore convention.

## Verification & Definition of Done
- [x] Design owner confirms the L1 and L100 ability designs and the draft set before the YAML is finalized.
- [x] Mechanic unit tests for `core:trade_bonus`, `core:villager_xp`, `core:barter_luck`, and the two summon mechanics.
- [x] Bundled-skill auto-sweeps pass; `trade.yml` parses and is shipped in `bundledSkills` and `gui.yml`.
- [x] `docs/users/capabilities.md` documents the new mechanics; `docs/users/creating-skills.md` shows a trade source.
- [x] `./gradlew build` and `./gradlew test` pass.