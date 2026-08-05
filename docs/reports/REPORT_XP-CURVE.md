# REPORT_XP-CURVE.md: XP-Curve Balance Research Report

**Status:** Research report (no production changes applied)
**Issue:** [ISSUE-107](../issues/ISSUE-107.md)
**Date:** 2026-08-01
**Inputs:** `src/main/resources/skills/*.yml` (32 bundled skills), `docs/dev/SKILL-DESIGN-FRAMEWORK.md`, `docs/dev/template-skill.yml`

---

## 1. Executive Summary

Every bundled skill uses the **same** progression curve (`polynomial`, `base_xp: 50`,
`exponent: 2.5`), so reaching level 100 always costs exactly **5,000,000 XP**. The curve
is perfectly consistent across skills by construction. What is **not** consistent is the
XP *reward* side: the per-action rewards and the real-world frequency of each trigger
differ enormously, producing a **21.5× spread** in estimated focused time-to-100. The spread runs
from **~390 hours** (riding) to **~8,300 hours** (alchemy), with a median near **~1,070 hours**.

The single biggest balance lever is not the curve but the XP-source reward values, several
of which look calibrated against trigger *rarity* (kills, ores) rather than trigger
*frequency* (tanking hits, block placement, item damage). Passive/tanking skills and
slow-crafting skills are dramatically under-rewarded.

---

## 2. Methodology

### 2.1 Curve math

The `polynomial` evaluator (`src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/PolynomialEvaluator.java`)
computes **cumulative** XP to a level as:

```
totalXp(level) = baseXp * level^exponent
```

`getLevelForXp(xp)` in `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/SkillDefinition.java`
returns the highest level whose cumulative requirement is `<=` the player's XP. With
`base_xp = 50`, `exponent = 2.5`:

| Milestone | Cumulative XP | Note |
|---|---|---|
| Level 1 | 50 | |
| Level 15 | 43,571 | |
| Level 25 | 156,250 | primary-ability tier |
| Level 50 | 883,883 | **82% of the journey is 50→100** |
| Level 76 | 2,500,000 | exact half of the total |
| Level 90 | 3,842,167 | |
| Level 99 | 4,875,936 | |
| Level 100 | 5,000,000 | last level alone: 124,064 XP |

The curve is a late-weighted power curve: the final 10 levels (90 to 100) demand
~1.16M XP (23% of the total). This is a deliberate "RuneScape-style" long tail and is
fine as a design pillar. The problem is purely the reward-side rates below.

### 2.2 Reward behavior modeled (current code)

Rewards are the raw constants in each skill's `xp_sources` in
`src/main/resources/skills/*.yml`. Per **ISSUE-105**, bulk triggers scale the reward by
operation size (`SkillEventListener.resolveEventBulkScalar`):

- `furnace_extract` multiplies the reward by `FurnaceExtractEvent.getItemAmount()`,
  the number of items extracted in one pull (4 ingots → 4× the reward).
- `collect_xp` multiplies by `PlayerExpChangeEvent.getAmount()`. This means
  `enchanting`'s `collect_xp: 1.0` and `wizardry`'s `collect_xp: 2.0` pay **1/2 skill XP
  per vanilla XP point** collected.
- `consume_item` is not bulk-scaled (scalar 1 per item consumed).

So the modeled "XP per action" below already reflects the live engine, not the nominal
constant. Where a source's real per-action yield differs from its nominal constant
(e.g. a `furnace_extract: 4.0` constant pays per extracted item), that is what is modeled.

### 2.3 Time-to-100 model

`hoursTo100 = 5,000,000 / (skillXPPerMinute × 60)`, where `skillXPPerMinute` is the sum
over XP sources of `reward-per-action × actions-per-minute`.

**Actions-per-minute assumptions** (focused, optimal "grinding" play by an experienced
player. Used identically for all skills. Deliberately conservative):

| Activity | Actions/min | Activity | Actions/min |
|---|---|---|---|
| Break soft blocks (dirt/gravel/sand) | 30 | Melee/ranged hits | 18 |
| Harvest crops / herbs | 40 / 30 | Kills (when spawning allows) | 2.5 |
| Mine stone | 25 | Take hits (tanking) | 8 |
| Fell logs | 18 | Craft bulk (planks, tools, wool) | 8 |
| Mine ore veins (effective) | 1.2 | Brew potion batches | 1 |
| Fish (reel-in) | 4 | Enchant items | 1.5 |
| Place blocks | 15 | Ride/mount | 5 |
| Collect vanilla XP (orb grinding) | 60 | Launch tridents / pearls | 6 / 3 |
| Breed / shear / tame | 2.5 / 3 / 1 | Eat cooked food (hunger-bound) | 1 |
| Totem resurrection | 0.1 | Item-durability events | 5 |

These are order-of-magnitude estimates, not measured telemetry. Halving or doubling any
rate changes absolute hours but does **not** change the relative ordering or the
conclusion about the spread (the ratios between skills are insensitive to the shared APM
assumptions). Casual (non-grinding) play would multiply every estimate by roughly
2-5×. The *relative* picture is unchanged.

---

## 3. Per-Skill Time-to-100 Estimates

All 32 skills: `progression: { curve: polynomial, base_xp: 50, exponent: 2.5 }`.
Sorted fastest → slowest.

| Skill (`src/main/resources/skills/`) | Dominant XP sources (reward per action) | Est. XP/min | Est. hours → 100 |
|---|---|---|---|
| `riding.yml` | ride_horse (25) + mounted damage (5) | 215 | **388** |
| `smithing.yml` | tool craft (12) + iron smelt (6×0.7) + gold smelt (8×1.0) | 178 | **468** |
| `dual_wield.yml` | damage (6) + kill (25) | 171 | **489** |
| `farming.yml` | crop harvest (4) | 160 | **521** |
| `unarmed.yml` | damage (5) + kill (25) | 153 | **546** |
| `heavy_weapons.yml` | damage (5) + kill (20) | 140 | **595** |
| `light_weapons.yml` | damage (5) + kill (20) | 140 | **595** |
| `one_handed.yml` | damage (5) + kill (20) | 140 | **595** |
| `throwing.yml` | trident launch (8) + damage (5) | 138 | **604** |
| `archery.yml` | arrow damage (5) + kill (15) | 128 | **654** |
| `woodcutting.yml` | log break (7) | 126 | **661** |
| `herbalism.yml` | herb break (4) | 120 | **694** |
| `carpentry.yml` | plank craft (8) + log break (2) | 100 | **833** |
| `excavation.yml` | soft-block break (3) | 90 | **926** |
| `tailoring.yml` | wool craft (10) + item damage (1) | 85 | **980** |
| `enchanting.yml` | enchant (15) + collect_xp (1/orb) | 83 | **1,010** |
| `building.yml` | place (2) + stone-brick craft (6) | 78 | **1,068** |
| `husbandry.yml` | breed (15) + shear (5) + tame (20) | 73 | **1,149** |
| `mining.yml` | ore break (15) + stone break (2) | 68 | **1,225** |
| `wizardry.yml` | collect_xp (2/orb) + pearl launch (3) | 49 | **1,701** |
| `fishing.yml` | fishing (10) | 40 | **2,083** |
| `shields.yml` | blocking damage-taken (5) | 40 | **2,083** |
| `unarmored.yml` | damage-taken unarmored (5) | 40 | **2,083** |
| `masonry.yml` | terracotta smelt (5×0.35) + glass smelt (4×0.1) + place (1) | 32 | **2,588** |
| `bard.yml` | instrument interact (2) + jukebox craft (10) | 25 | **3,333** |
| `heavy_armor.yml` | damage-taken heavy (3) | 24 | **3,472** |
| `light_armor.yml` | damage-taken light (3) | 24 | **3,472** |
| `medium_armor.yml` | damage-taken medium (3) | 24 | **3,472** |
| `piety.yml` | breed (8) + resurrect (30) | 23 | **3,623** |
| `acrobatics.yml` | damage-taken on ground (3) | 15 | **5,556** |
| `cooking.yml` | cook food (4×0.35) + eat (2) | 13 | **6,313** |
| `alchemy.yml` | brew potion (10) | 10 | **8,333** |

**Ordered clusters:** combat-melee (5-6 XP/hit + 20-25/kill) and high-frequency gathering
(farming, woodcutting, herbalism) land in a healthy **390-700 h** band. Crafting/profession
skills sit in the **800-1,700 h** band. Everything driven by *taking damage*, *brewing*,
*eating*, or low-frequency events (totem, jukebox) is **3,000-8,300 h**. This is beyond any
reasonable content-completion horizon.

---

## 4. Consistency & Fairness Assessment

**Curve:** uniform and fair. One curve, no per-skill favorites. Milestone tiers
(`docs/dev/SKILL-DESIGN-FRAMEWORK.md` §3) are respected by the math (25/50/75/100 sit at
156k/884k/2.44M/5M XP).

**Rewards:** not fair. Findings:

1. **21.5× spread** in time-to-100. A player completing the fastest skill in 400 h would
   need ~20× that for the slowest. The slowest skills are effectively uncapped grind.
2. **Passive tanking skills are under-rewarded.** All four armor skills (`heavy_armor`,
   `medium_armor`, `light_armor`, `unarmored`) grant only **3-5 XP per hit taken**. Taking
   hits is both rarer and more dangerous than landing them. `acrobatics` (3 XP/hit)
   compounds this with a ground-only condition.
3. **ISSUE-105 scaling devalued furnace skills. Corrected to item-count scaling.** The
   initial ISSUE-105 scalar scaled `furnace_extract` by stored furnace XP, paying
   `cooking`'s nominal `4.0`/`masonry`'s `5.0`/`4.0` as ≈1.4/1.75/0.4 XP per item. ISSUE-173
   corrected the scalar to `getItemAmount()` (items extracted), so the nominal constants
   now pay their full value per extracted item. The bundled furnace constants should be
   re-tuned for the new effective yields (see P2 below).
4. **Low-frequency triggers are over-rewarded on paper but irrelevant in practice.**
   `resurrect` (30 XP, totem activation) and `ride_horse` (25 XP) look generous but their
   events are so rare/sporadic that they still rank poorly or distort the source mix.
5. **`collect_xp` passive drift.** `enchanting`/`wizardry` gain skill XP from *any* vanilla
   orb the player collects, so they quietly outpace their intended activity.

---

## 5. External Baselines

### RuneScape (publicly documented XP table)
- XP to level 99 is **13,034,431**. **Level 92 is exactly half** of 99's total. This is a curve
  Skilling's (half at level 76) mirrors in spirit.
- Community-documented training times: fast skills (Cooking, Firemaking) **≈30-100 h** to
  99. Slow skills (Mining, Agility, Runecraft) **≈300-500 h**. Even RuneScape's slowest
  skills stay within a ~10× band of its fastest, and its slow skills have
  high-frequency, bulkable training methods.
- **Takeaway:** a 21.5× spread with no fast method for the slow skills is worse than the
  genre's most notorious grind game.

### Valheim
- Skills run 0-100 with **no fixed total**. Each skill use grants small XP and gains
  diminish steeply past ~70-80. Maxing a skill is a long tail, but *every* skill has a
  continuous, high-frequency training loop (hit things, mine, craft, swim).
- **Takeaway:** the "no dead-end skill" principle. Every skill must have a repeatable
  high-frequency training loop. Skilling's tanking/brewing/eating skills violate this.

### mcMMO (documented design)
- Skills cap at level **1000**. XP-per-level grows roughly quadratically. Each skill's XP
  gain is independently configurable and tuned so grinding the *activity* (not waiting)
  is always the fastest path.
- **Takeaway:** per-skill reward tuning is the expected norm. A single shared curve with
  wildly varying rewards is the exception, not the baseline.

### AuraSkills (documented design)
- Fully configurable XP curve + per-action XP. Servers commonly set a target
  "hours-to-max" per skill by back-computing reward values.
- **Takeaway:** the *method* of back-computing rewards from a target time (used in §7) is
  the established practice.

---

## 6. Assessment Against Design Framework

Per `docs/dev/SKILL-DESIGN-FRAMEWORK.md`, skills should be *roughly* equivalent time
investments with milestone pacing. Today only the curve is equivalent. The rewards are
not. The `Lore Clarity` convention (ISSUE-109) makes `&7Costs`/`&8Requires` visible, but
costs (hunger, cooldowns) also gate ability use and should be included when estimating
reward-per-hour in future balance passes.

---

## 7. Suggested Adjustments (proposals. NOT applied)

### 7.1 Target band

Pick a single target: **focused ~500 h, casual ~1,500-2,000 h to level 100** (aligned with
the healthy combat/gathering cluster and RuneScape's slower-skills band). Every skill
should land within roughly **2×** of the band median.

### 7.2 Concrete, prioritized proposals (follow-up issues)

**P1: Bring the slowest skills up (biggest fairness win).**
- Armor/tanking skills: raise `entity_damage_taken` rewards from **3 → ~15-20 XP/hit**
  (`heavy_armor.yml`, `medium_armor.yml`, `light_armor.yml`, `unarmored.yml`,
  `acrobatics.yml`), and drop the ground-only condition on `acrobatics.yml`.
- `alchemy.yml`: `brew_potion` **10 → ~40 XP/batch** (batches are rare and expensive).
- `cooking.yml`: re-tune after ISSUE-105. Target **~30-40 XP/meal-equivalent** by
  raising both the `furnace_extract` constant and adding a `craft_item`/`furnace_extract`
  source that pays on bulk output.
- `bard.yml`: raise `player_interact` instrument **2 → ~10 XP** and make jukebox
  `craft_item` a steady source.
- `piety.yml`: `breed_animals` **8 → ~25 XP**. Keep `resurrect` as a bonus, not the spine.

**P2: Re-tune furnace skills for the ISSUE-105 scalar.**
- Recompute `masonry.yml`/`smithing.yml`/`cooking.yml` furnace constants so the
  *effective* (scaled) yield is what the designer intends. E.g. to yield 20 XP per
  smelted iron ore (0.7 stored XP) the constant must be **≈29**, not 6.

**P3: Slightly rebalance the fastest skills.**
- `riding.yml` `ride_horse` **25 → ~15**, mounted damage **5 → 4**.
- `smithing.yml` tool craft **12 → ~10**. `dual_wield.yml` kill **25 → ~20**.

**P4: Decision on `collect_xp` passive drift.**
- Either scope `enchanting`/`wizardry` `collect_xp` behind a filter (e.g. enchanting at a
  table) or accept it as the skill's "always-on" income and lower the constant.

### 7.3 Framework for designing XP source rewards

For any new or edited skill, calibrate **reward per action** from a target time:

1. Set the skill's target **hours-to-100** (use the band in §7.1).
2. Convert to **skill XP/min**: `XPmin = 5,000,000 / (hours × 60)`.
3. Identify the skill's dominant trigger and its **actions/min** (use §2.3 table as a
   starting point. Add a new row if the trigger is novel).
4. `reward = XPmin / actionsPerMin`. Split across sources if the skill has several.
5. **Adjust for rarity & bulk:** multiply by event rarity (`resurrect`, `tame`) or divide
   by operation size (furnace/collect scalars, ISSUE-105) so the *effective* per-action
   yield matches step 4.
6. **Adjust for gating costs:** if the ability consumes hunger/items (ISSUE-109 costs),
   add a floor so the player's profit from using it stays positive.
7. Verify the milestone shape: level 25, 50, 75, 100 should be reachable in
   **≈3 %, ≈18 %, ≈49 %, 100 %** of the target time respectively (matches the curve's
   cumulative fractions: 156k/884k/2.44M/5M XP).

This mirrors the back-computation practice used by AuraSkills servers and gives every
skill a defensible, auditable number.

---

## 8. Verification of DoD

- [x] Methodology documented (assumptions, rates, hours-per-level math). §2.
- [x] Every bundled skill has a time-to-100 estimate with reasoning. §3 (all 32 files
  listed with real paths under `src/main/resources/skills/`).
- [x] Comparison section covers mcMMO and AuraSkills plus RuneScape and Valheim. §5.
- [x] Concrete, prioritized suggestions + a reusable reward-design framework. §7.
- [x] Clean Markdown. Cross-references to skill YAML use real file paths.

**Follow-ups to file:** §7.2 P1-P4 as `feat(skills)` backlog issues (reward re-tune),
referencing this report.
