# REPORT: XP-Source Design — Multi-Source, 50-Hour-to-100 Target

**Owning ticket:** [ISSUE-177](../issues/ISSUE-177.md)
**Date:** 2026-08-02
**Type:** Research / design plan (no production code changes)
**Inputs:** all 32 bundled skills, [REPORT_XP-CURVE.md](REPORT_XP-CURVE.md), `docs/users/capabilities.md`, `src/main/resources/tags.yml`, [ISSUE-176](ISSUE-176.md)

---

## 1. Current state: catalog of all 32 skills

All skills share `progression: { curve: polynomial, base_xp: 50, exponent: 2.5 }` →
**5,000,000 XP** to level 100. Source counts today (trigger `[reward]` + filters):

| Skill | Sources today | Single-source? |
|---|---|---|
| `acrobatics` | `entity_damage_taken[3] is_on_ground` | ✅ |
| `alchemy` | `brew_potion[10]` | ✅ |
| `archery` | `entity_damage[5] #arrows` · `entity_kill[15] #arrows` | |
| `bard` | `player_interact[2] #c:instruments` · `craft_item[10] jukebox` | |
| `building` | `block_place[2] #c:stone_products` · `craft_item[6] #minecraft:stone_bricks` | |
| `carpentry` | `craft_item[8] planks` · `block_break[2] logs` | |
| `cooking` | `furnace_extract[4] cooked_beef` · `consume_item[2] cooked_beef` | |
| `dual_wield` | `entity_damage[6] offhand:weapon` · `entity_kill[25] offhand:weapon` | |
| `enchanting` | `enchant_item[15]` · `collect_xp[1]` | |
| `excavation` | `block_break[3] #c:excavatable` | ✅ |
| `farming` | `block_break[4] #minecraft:crops` | ✅ |
| `fishing` | `fishing[10]` | ✅ |
| `heavy_armor` | `entity_damage_taken[3] equipped:heavy` | ✅ |
| `heavy_weapons` | `entity_damage[5] #c:heavy_weapons` · `entity_kill[20] #c:heavy_weapons` | |
| `herbalism` | `block_break[4] #c:herbs` | ✅ |
| `husbandry` | `breed_animals[15]` · `player_shear[5]` · `player_tame[20]` | |
| `light_armor` | `entity_damage_taken[3] equipped:light` | ✅ |
| `light_weapons` | `entity_damage[5] #c:light_weapons` · `entity_kill[20] #c:light_weapons` | |
| `masonry` | `furnace_extract[5] terracotta` · `furnace_extract[4] glass` · `block_place[1] #c:stone_products` | |
| `medium_armor` | `entity_damage_taken[3] equipped:medium` | ✅ |
| `mining` | `block_break[15] #c:ores` · `block_break[2] #c:stone` | |
| `one_handed` | `entity_damage[5] offhand:empty+swords` · `entity_kill[20] offhand:empty+swords` | |
| `piety` | `breed_animals[8]` · `resurrect[30]` · `player_interact[6] dirt+bone` (ISSUE-176) | |
| `riding` | `ride_horse[25]` · `entity_damage[5] is_riding` | |
| `shields` | `entity_damage_taken[5] is_blocking` | ✅ |
| `smithing` | `craft_item[12] #c:tools` · `furnace_extract[6] iron_ores` · `furnace_extract[8] gold_ores` | |
| `tailoring` | `craft_item[10] wool` · `item_damage[1] #c:leather_armor` | |
| `throwing` | `launch_projectile[8] trident` · `entity_damage[5] trident` | |
| `unarmed` | `entity_damage[5] hand:empty` · `entity_kill[25] hand:empty` | |
| `unarmored` | `entity_damage_taken[5] armor:empty` | ✅ |
| `wizardry` | `collect_xp[2]` · `launch_projectile[3] ender_pearl` | |
| `woodcutting` | `block_break[7] #minecraft:logs` | ✅ |

**13 of 32 skills are single-source**, and the armor/tanking cluster is both single-source
and under-rewarded. `REPORT_XP-CURVE.md` §3 measured the spread: 388 h (`riding`) to
8,333 h (`alchemy`), median ~1,070 h — a 21.5× band.

## 2. The 50-hour target math and the curve decision

| Quantity | Value |
|---|---|
| Total XP to 100 (shared curve) | 5,000,000 |
| Target | **50 h** |
| Sustained rate | 5,000,000 / (50 × 60) = **1,667 XP/min** (≈28 XP/s) |
| Milestone pacing | 25/50/75/100 = 156k/884k/2.44M/5M XP = **3 %/18 %/49 %/100 %** of the journey |

1,667 XP/min is **~8× today's fastest skill** (215 XP/min `riding`) and **~20× faster** than
the ~500 h band recommended in `REPORT_XP-CURVE.md` §7.1. A strict, uniform 50 h for every
skill forces reward constants that are mechanically absurd for low-frequency skills
(e.g. 667 XP per `alchemy` brew batch, 256 XP per `husbandry` breed).

**Recommendation: (c) hybrid — keep the shared curve, compute rewards from the 50 h
target, and prefer *adding high-frequency sources* over inflated per-action rewards.**

- Keep the polynomial curve untouched (brand consistency, milestone fractions, the
  RuneScape-style tail are design pillars).
- For each skill, compute the uniform per-action rate `R = 1,667 / Σ APM` over the skill's
  (expanded) source set. This is the transparent, back-computed constant.
- **Outlier rule:** if `R` exceeds a sanity ceiling (≈60 XP/action for repeatable actions,
  ≈150 XP/action for rare batches/kills), the skill's assumed action rate is too low —
  **add another high-frequency source** (gathering, crafting, placement) to raise Σ APM and
  pull `R` down, rather than inflating the existing constant. Skills that *cannot* be made
  high-frequency (e.g. `alchemy`) accept a longer effective time (≈80–120 h) — this is the
  honest residual gap and is documented per skill.

## 3. Reward formula

```
R = XP_MIN / Σ APM           (uniform per-action constant for the skill)
XP_MIN = 5,000,000 / (50 × 60) = 1,666.7
APM_i = actions/minute for source i (REPORT_XP-CURVE §2.3 table)
```

- **Bulk-scaled triggers** (`furnace_extract` pays per item, `collect_xp` per orb):
  `R` is the per-item/per-orb rate; the per-action yield is `R × itemsPerAction`.
- Reward is split **uniformly**; a rarity-weighted variant (allocate the XP/min budget
  across sources before dividing by APM) is a valid refinement for rare-event skills
  (`resurrect`, `tame`, villager cure) where the uniform method over-rewards common sources.

## 4. Per-skill expanded source design (primary + secondary + tertiary)

`R` values are the uniform 50 h constants (one decimal). "XP/min" is the modelled sustained
yield (sum of `R × APM`), which equals ≈1,667 for every skill by construction.

| Skill | Sources (trigger / filters) | APM | R | XP/min |
|---|---|---|---|---|
| `acrobatics` | `entity_damage_taken` `is_on_ground` · `entity_damage_taken` (any) · `launch_projectile` ender_pearl | 8/4/3 | 111.1 | 1,667 |
| `alchemy` | `brew_potion` · `entity_kill` `#c:zombies` · `consume_item` `#c:potions` | 1/0.5/1 | 666.7 | 1,667 |
| `archery` | `entity_damage` `#minecraft:arrows` · `entity_kill` `#minecraft:arrows` · `shoot_bow` `#c:bows` | 18/2.5/12 | 51.3 | 1,667 |
| `bard` | `player_interact` `#c:instruments` · `craft_item` jukebox · `block_place` jukebox | 15/0.5/15 | 54.6 | 1,667 |
| `building` | `block_place` `#c:stone_products` · `craft_item` `#minecraft:stone_bricks` · `block_place` `#minecraft:planks` | 15/8/15 | 43.9 | 1,667 |
| `carpentry` | `craft_item` planks · `block_break` logs · `craft_item` `#c:wooden_products` | 8/18/4 | 55.6 | 1,667 |
| `cooking` | `furnace_extract` `#c:foods` · `consume_item` `#c:foods` · `furnace_extract` cooked_beef | 8/1/4 | 128.2 | 1,667 |
| `dual_wield` | `dualwield:offhand_attack` (ISSUE-175) · `dualwield:offhand_attack` kill · `entity_damage` `offhand:weapon` | 18/2.5/18 | 43.3 | 1,667 |
| `enchanting` | `enchant_item` · `collect_xp` · `repair` | 1.5/60/2 | 26.2 | 1,667 |
| `excavation` | `block_break` `#c:excavatable` · `block_break` `#minecraft:sand` · `craft_item` `#c:shovels` | 30/10/8 | 34.7 | 1,667 |
| `farming` | `block_break` `#minecraft:crops` · `block_break` `#c:crops` · `craft_item` wheat | 40/20/8 | 24.5 | 1,667 |
| `fishing` | `fishing` · `fishing` (treasure) · `craft_item` `#c:fishing_rods` | 4/0.5/8 | 133.3 | 1,667 |
| `heavy_armor` | `entity_damage_taken` `equipped:heavy` · `entity_damage_taken` `is_blocking` · `craft_item` `#c:heavy_armor` | 8/4/4 | 104.2 | 1,667 |
| `heavy_weapons` | `entity_damage` `#c:heavy_weapons` · `entity_kill` `#c:heavy_weapons` · `entity_damage` (sweep) | 18/2.5/6 | 62.9 | 1,667 |
| `herbalism` | `block_break` `#c:herbs` · `block_break` `#minecraft:flowers` · `craft_item` `#c:herbs` | 30/20/4 | 30.9 | 1,667 |
| `husbandry` | `breed_animals` · `player_shear` · `player_tame` | 2.5/3/1 | 256.4 | 1,667 |
| `light_armor` | `entity_damage_taken` `equipped:light` ×2 · `craft_item` `#c:leather_armor` | 8/2/4 | 119.0 | 1,667 |
| `light_weapons` | `entity_damage` `#c:light_weapons` · `entity_kill` · `entity_damage` (crit) | 18/2.5/6 | 62.9 | 1,667 |
| `masonry` | `furnace_extract` terracotta · `furnace_extract` glass · `block_place` `#c:stone_products` | 8/8/15 | 53.8 | 1,667 |
| `medium_armor` | `entity_damage_taken` `equipped:medium` · `entity_damage_taken` `is_blocking` · `craft_item` iron_ingot | 8/4/4 | 104.2 | 1,667 |
| `mining` | `block_break` `#c:stone` · `block_break` `#c:ores` · `block_break` `#minecraft:deepslate` | 25/1.2/15 | 40.5 | 1,667 |
| `one_handed` | `entity_damage` `offhand:empty`+swords · `entity_kill` · `craft_item` swords | 18/2.5/4 | 68.0 | 1,667 |
| `piety` | see §6 (bury, undead, totem, cure, holy placement) | 27.8 | 60.0 | 1,667 |
| `riding` | `ride_horse` · `entity_damage` `is_riding` · `player_tame` | 5/18/1 | 69.4 | 1,667 |
| `shields` | `entity_damage_taken` `is_blocking` · `entity_kill` `is_blocking` · `craft_item` `#c:shields` | 8/2.5/4 | 114.9 | 1,667 |
| `smithing` | `craft_item` `#c:tools` · `furnace_extract` iron_ores · `furnace_extract` gold_ores · `repair` | 8/8/8/2 | 64.1 | 1,667 |
| `tailoring` | `craft_item` wool · `craft_item` `#c:leather_armor` · `item_damage` `#c:leather_armor` | 8/6/5 | 87.7 | 1,667 |
| `throwing` | `launch_projectile` trident · `entity_damage` trident · `entity_kill` trident | 6/18/2.5 | 62.9 | 1,667 |
| `unarmed` | `entity_damage` `hand:empty` · `entity_kill` `hand:empty` · `entity_damage_taken` `armor:empty` | 18/2.5/4 | 68.0 | 1,667 |
| `unarmored` | `entity_damage_taken` `armor:empty` ×2 · `entity_damage` `hand:empty` | 8/4/6 | 92.6 | 1,667 |
| `wizardry` | `collect_xp` · `launch_projectile` ender_pearl · `enchant_item` | 60/3/1.5 | 25.8 | 1,667 |
| `woodcutting` | `block_break` logs · `block_break` leaves · `craft_item` planks | 18/20/8 | 36.2 | 1,667 |

**Outliers flagged by the sanity rule** (R > 60/action): `alchemy` (667), `husbandry` (256),
`fishing` (133), `cooking` (128), all armor/tanking skills (93–119). For these, execution
tickets should **add high-frequency sources first** (gather herbs for `alchemy`, shear/
wool loops for `husbandry`, `block_place`/`craft_item` loops for the armor skills) to pull
R down, and only then accept a residual ~80–120 h effective time rather than inflating a
single action to absurd levels.

## 5. Tag expansion (`src/main/resources/tags.yml`)

New custom tags referenced by the design (vanilla cross-refs are stable through 1.21):

| New `c:` tag | Contents (vanilla refs) | Used by |
|---|---|---|
| `c:undead` | `#minecraft:zombies`, `#minecraft:skeletons`, `minecraft:wither_skeleton`, `minecraft:phantom`, `minecraft:zombified_piglin`, `minecraft:drowned`, `minecraft:stray`, `minecraft:husk` | `piety` undead kills, `alchemy` |
| `c:holy_blocks` | `minecraft:beacon`, `minecraft:conduit`, `minecraft:lantern`, `minecraft:soul_lantern`, `minecraft:sea_lantern`, `minecraft:glowstone`, `minecraft:gold_block` | `piety` `block_place` |
| `c:crops` | `#minecraft:crops`, `minecraft:sugar_cane`, `minecraft:melon`, `minecraft:pumpkin`, `minecraft:cocoa` | `farming` tertiary |
| `c:foods` | `minecraft:cooked_beef`, `#minecraft:food` (1.21 has `#minecraft:food`), plus cooked variants | `cooking` |
| `c:potions` | `minecraft:potion`, `minecraft:splash_potion`, `minecraft:lingering_potion`, `minecraft:honey_bottle` | `alchemy` consume |
| `c:shields` | `minecraft:shield` | `shields` |
| `c:heavy_armor` | `#minecraft:chest_armor`+`#minecraft:leg_armor` tiers (iron+), or explicit iron/diamond/netherite pieces | `heavy_armor` craft |
| `c:wooden_products` | `#minecraft:planks`, `#minecraft:wooden_slabs`, `#minecraft:wooden_stairs`, `minecraft:crafting_table`, `minecraft:chest`, `minecraft:ladder` | `carpentry` tertiary |
| `c:shovels` | `#minecraft:shovels` | `excavation` |
| `c:bows` | (already exists) | `archery` shoot |
| `c:fishing_rods` | (already exists) | `fishing` rod craft |

## 6. Fully-worked example: `piety`

Five sources (primary + 4):

| # | Source | Filter | APM | R (uniform) | XP/min |
|---|---|---|---|---|---|
| 1 | `player_interact` (bury bone) | `target #minecraft:dirt`, `tool minecraft:bone` | 10 | 60.0 | 600 |
| 2 | `entity_kill` (undead) | `target #c:undead` | 2.5 | 60.0 | 150 |
| 3 | `block_place` (holy block) | `target #c:holy_blocks` | 15 | 60.0 | 900 |
| 4 | `player_interact`/`resurrect` (totem) | `resurrect` | 0.1 | 60.0 | 6 |
| 5 | villager cure | new trigger or `player_interact`+state | 0.2 | 60.0 | 12 |

Σ APM = 27.8 → **R = 1,666.7 / 27.8 ≈ 60.0** per action. This is the derived constant for
every piety source under the 50 h target. Note the tension with ISSUE-176, which shipped
bury at **6 XP**: the 50 h target implies **10×** that (60 XP) — the execution ticket should
either adopt the derived constant or keep bury low and lean on the two high-frequency
sources (bury 600/min + holy placement 900/min) to carry the skill.

## 7. Engine capability gaps the plan depends on

1. **`target_type` on `entity_kill` / `EntityDeathEvent`** — today the `target_type` state
   filter only matches `EntityDamageByEntityEvent` (`Skilling.java:424-431`). Undead-kill
   sourcing needs it extended to `EntityDeathEvent` (or a `target` entity-type filter on
   `entity_kill`).
2. **Villager-cure trigger** — no trigger exists. Paper exposes a villager-cure event
   (verify exact class on the target API); otherwise model it as `player_interact` with a
   `#c:zombie_villagers`-style target + `hand`/`tool` golden-apple filter.
3. **`dualwield:offhand_attack` / `dualwield:offhand_block_break`** — new triggers from
   ISSUE-175 (with base-dispatch suppression).
4. **Block-target filters on `player_interact`** — shipped in ISSUE-176 (bury).
5. **`repair` trigger / `furnace_extract` bulk semantics** — `repair` exists
   (`PrepareAnvilEvent`); furnace constants must be tuned to the per-item scalar
   (REPORT_XP-CURVE §7.2 P2).

**Follow-up implementation tickets (backlog):**
- `feat(skills)`: rework the 13 single-source skills to the multi-source table above.
- `feat(skills)`: apply the derived reward constants to all 32 skills (one ticket per
  cluster: combat / gathering / crafting / tanking).
- `feat(engine)`: extend `target_type` to `EntityDeathEvent`; add villager-cure trigger;
  add the §5 `c:` tags to `tags.yml`.
- `feat(skills)`: `piety` adopt the §6 source set (with ISSUE-176's bury retained).

## 8. Reconciliation with REPORT_XP-CURVE.md

- **Curve:** unchanged (5M XP; milestone fractions 3 %/18 %/49 %/100 % preserved), matching
  the framework's milestone pacing. At 50 h: level 25 ≈ 1.5 h, level 50 ≈ 9 h, level 75 ≈
  24.5 h, level 100 = 50 h.
- **Rewards:** this plan deliberately supersedes the ~500 h band in REPORT_XP-CURVE §7.1 with
  the sprint's 50 h directive (~10× higher constants). The two reports agree on the *method*
  (§7.3 back-computation) and on the *findings* (single-source and tanking skills are the
  problem); they differ only on the target time, which is an explicit product decision.
- **Outlier skills** (`alchemy`, `husbandry`, armor/tanking) remain the hardest to reach
  50 h; the hybrid rule keeps their constants within a defensible ceiling and documents a
  residual 80–120 h effective time.

## Verification of DoD

- [x] All 32 skills covered with an expanded (primary + secondary + tertiary) source set — §4.
- [x] 50 h reward math and per-source reward-constant table (skill → trigger → filter →
      reward → XP/min) — §2, §3, §4.
- [x] Fully-worked `piety` example (bury via ISSUE-176, undead kills, villager curing, totem
      activations, holy-block placement) — §6.
- [x] `tags.yml` additions and engine-capability gaps + follow-up tickets — §5, §7.
- [x] Reconciled with REPORT_XP-CURVE.md (milestone pacing, action-rate table) — §8.
- [x] Clean Markdown; no production code changed by this ticket.
