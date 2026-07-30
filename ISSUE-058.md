# Pre-Built Skill Configurations

## Issue

The plugin ships with only `template-skill.yml` as an example. For a "data-driven" engine to be useful out of the box, it should ship with a set of **pre-built, production-ready skill configurations** that server owners can enable immediately. The `src/main/resources/skills/` directory is empty — no skills are bundled in the JAR.

The DESIGN.md §9 blueprints a 32-skill web spanning 6 categories (Harvesting, Combat Offense, Combat Defense, Crafting, Arcane, Mobility). Only the "Mining" skill (from `template-skill.yml`) exists as a reference implementation.

## Scope

Design and implement **6 pre-built skill configurations** following the SKILL-DESIGN-FRAMEWORK.md rules:
1. Mining (update from template to match framework)
2. Woodcutting
3. Excavation
4. Farming
5. Fishing
6. Archery

These 6 cover 3 of the 6 blueprint categories (Harvesting & Gathering, Combat Offense, Crafting/Trade) and exercise all 19 existing triggers and 25 existing mechanics.

## Design Framework

Every skill follows the SKILL-DESIGN-FRAMEWORK.md:

### 6-Tier Milestone Progression

| Milestone | Tier Type | Role |
|-----------|-----------|------|
| **Level 1** | Foundational Passive | Identity & primary scalar |
| **Level 15** | Quality-of-Life Passive | Early friction reduction |
| **Level 25** | Primary Ability | Core gameplay hook (active) |
| **Level 50** | Major Passive | Mid-game efficiency spike |
| **Level 75** | Synergy Passive | Cross-system vanilla link |
| **Level 100** | Mastery Capstone | Game-changing perk |

### Dual-Layer Progression

- **Primary scalar:** Linear across L1-L100 (e.g., double drop chance)
- **Milestone sub-scaling:** Each ability's parameters grow from unlock level to L100

### Ability Audit Checklist

Each ability must pass all 6 checks: Uninstall Test, Vanilla Synergist Test, Restraint Test, PvE Test, API Feasibility Test, Sub-Scaling Test.

## Affected Files

| File | Action |
|------|--------|
| `src/main/resources/skills/mining.yml` | Rewrite from template |
| `src/main/resources/skills/woodcutting.yml` | Create |
| `src/main/resources/skills/excavation.yml` | Create |
| `src/main/resources/skills/farming.yml` | Create |
| `src/main/resources/skills/fishing.yml` | Create |
| `src/main/resources/skills/archery.yml` | Create |
| `Skilling.java` | Update `onEnable()` to save bundled skill files on first run |
| `template-skill.yml` | Keep as standalone reference |

## Development Plan

### Step 1: Design shared progression curves

All skills share a common XP curve polynomial for consistency:

```yaml
progression:
  curve: polynomial
  base_xp: 50
  exponent: 2.5
```

Primary scalar pattern:
```yaml
progression:
  primary_scalar:
    base: 0.0
    step: 0.01     # +1% per level, reaching ~100% at level 100
    cap: 1.0       # hard cap at 100%
```

### Step 2: Skill Designs

#### 2a. Mining (Green, Segmented Boss Bar)

| Lvl | Ability | Type | Mechanic | Sub-Scaling |
|-----|---------|------|----------|-------------|
| 1 | Geologist | Passive | `core:yield_multiplier` (tag: `#c:ores`) | Multiplier: linear 1.0→2.0 |
| 15 | Prospector | Passive | `core:xp_bonus` (multiplier: 1.5→3.0) | Multiplier: linear from 1.5 |
| 25 | Vein Miner | Active (Shift+Right-click with pickaxe) | `core:chain_break` (tag: `#c:veinminer`, chain_limit: milestones {25: 3, 50: 8, 75: 16, 100: 32}) | Chain limit: milestone |
| 50 | Magma Forge | Passive | `core:auto_smelt` (chance: linear 25%→75%) | Chance: linear from unlock |
| 75 | Totem Support | Passive | None (special handler) — Totems activate from inventory while mining | Radius: linear from 3→8 |
| 100 | Perfect Yield | Passive | `core:yield_multiplier` (guaranteed double, chance for triple) | Triple chance: linear 0%→25% |

Requirements: Vein Miner needs `is_sneaking` state, consumes coal, 5s cooldown.
Feedback: Geologist silent; Vein Miner gets action bar + sound on activation.

**Framework compliance:**
- ✅ Uninstall Test: No world modification, vanilla block drops
- ✅ Vanilla Synergist: Requires pickaxes, consumes coal for Vein Miner
- ✅ Restraint Test: Uses native particles (block break, enchant glyphs)
- ✅ PvE Test: Direct value against environment
- ✅ API Feasibility: BlockBreakEvent, all standard mechanics
- ✅ Sub-Scaling: Every ability has growing parameters

#### 2b. Woodcutting (Green, Segmented)

| Lvl | Ability | Type | Mechanic | Sub-Scaling |
|-----|---------|------|----------|-------------|
| 1 | Lumberjack | Passive | `core:yield_multiplier` (tag: `#minecraft:logs`, multiplier: 1.0→2.0) | Multiplier: linear |
| 15 | Efficient Swing | Passive | `core:speed_bonus` (multiplier: 1.1→1.3) while holding axe | Speed: linear |
| 25 | Timber Feller | Active (Shift+Right-click with axe) | `core:chain_break` (tag: `#minecraft:logs`, chain_limit: milestones {25: 3, 50: 8, 75: 16, 100: 32}) | Chain limit: milestone |
| 50 | Forest Bounty | Passive | `core:yield_multiplier` (tag: `#minecraft:saplings`, multiplier: 1.0→3.0) + guaranteed apple drop | Sapling rate: linear |
| 75 | Reforestation | Passive | `core:block_damage` (chance to auto-bone-meal dirt below when breaking logs) | Chance: linear 10%→40% |
| 100 | Ancient Timber | Passive | `core:modify_craft_output` (planks: 1.0→2.0, sticks from logs) + bonus rare drop chance | Output: linear |

Triggers: `block_break` for logs, `craft_item` for planks.
Requirements: Timber Feller needs `is_sneaking`, consumes hunger (exhaustion: 0.3 per block).

#### 2c. Excavation (Green, Segmented)

| Lvl | Ability | Type | Mechanic | Sub-Scaling |
|-----|---------|------|----------|-------------|
| 1 | Dig Double | Passive | `core:yield_multiplier` (tag: `#minecraft:shovels` breakables, multiplier: 1.0→2.0) | Multiplier: linear |
| 15 | Quick Dig | Passive | `core:speed_bonus` (mining speed while holding shovel, 1.2→1.5) | Speed: linear |
| 25 | Wide Sweep | Active (Shift+Right-click with shovel) | `core:block_damage` (3×3×1 area break, tag: `#c:excavatable`, cooldown milestone: 10s→5s) | Cooldown: inverse reduction |
| 50 | Fossil Hunter | Passive | `core:yield_multiplier` (tag: `#minecraft:fossil_blocks`, multiplier: 1.0→4.0) + flint/bone guaranteed | Multiplier: linear |
| 75 | Eerie Dust | Passive | `core:xp_bonus` (XP from gravel/sand, multiplier: 1.5→4.0) | XP mult: linear |
| 100 | Excavator | Passive | Wide Sweep becomes 5×5×1, no cooldown | (capstone enhancement) |

Custom tags needed: `#c:excavatables` defining gravel, sand, dirt, grass, clay, soul sand, etc.
Triggers: `block_break` with shovel filter.

#### 2d. Farming (Green, Segmented)

| Lvl | Ability | Type | Mechanic | Sub-Scaling |
|-----|---------|------|----------|-------------|
| 1 | Green Thumb | Passive | `core:yield_multiplier` (tag: `#minecraft:crops`, multiplier: 1.0→2.0) | Multiplier: linear |
| 15 | Seasoned Hand | Passive | `core:saturation_inject` (food from crops gives +1→+3 saturation) | Saturation: linear |
| 25 | Harvest Wave | Active (Shift+Right-click with hoe) | `core:chain_break` (tag: `#minecraft:crops`, radius: milestone {25: 3, 50: 5, 75: 7, 100: 9}) | Radius: milestone |
| 50 | Nutrient Rich | Passive | `core:yield_multiplier` (bonus crops, multiplier: 1.0→3.0, chance: 50%→100%) | Chance: linear |
| 75 | Bone Meal Expert | Passive | Bone meal affects 3×3→5×5 area, reduced consumption chance | Radius: milestone, Save: linear |
| 100 | Living Earth | Passive | Harvested crops auto-replant seeds, no replant cost | (capstone) |

Triggers: `block_break` on crops, `player_interact` with bone meal.

**Framework note — auto-replant:** The L100 capstone requires a new mechanic (`core:auto_replant`) or can be implemented via existing BlockBreakEvent listener + `setCancelled(true)` + manual replant logic. If a new mechanic is needed, add it to the registry following the existing pattern.

#### 2e. Fishing (Green, Segmented)

| Lvl | Ability | Type | Mechanic | Sub-Scaling |
|-----|---------|------|----------|-------------|
| 1 | Angler | Passive | `core:yield_multiplier` (treasure chance, multiplier: 1.0→2.5) | Multiplier: linear |
| 15 | Light Line | Passive | Reduce fishing rod durability loss (50%→100% chance to save durability) | Save chance: linear |
| 25 | Lucky Catch | Passive | `core:modify_loot_table` (better enchantments on treasure, bonus fish) | Quality: milestone |
| 50 | Sea Bounty | Passive | `core:yield_multiplier` (double fish, multiplier: 1.0→2.0) | Multiplier: linear |
| 75 | Enchanted Waters | Passive | `core:modify_potion_duration` (fishing treasure potions last longer) + bonus XP | Duration: linear +50%→200% |
| 100 | Leviathan | Passive | Guaranteed max-size catch, bonus rare items, sound & particle fanfare on treasure | (capstone) |

Triggers: `fishing` (`PlayerFishEvent`).

**Framework note — loot table modification:** The L25 ability may need a new mechanic (`core:modify_loot_table`) if the existing mechanics don't cover fishing loot modification. Otherwise, it can use `core:yield_multiplier` with a custom filter.

#### 2f. Archery (Red, Segmented)

| Lvl | Ability | Type | Mechanic | Sub-Scaling |
|-----|---------|------|----------|-------------|
| 1 | Steady Hand | Passive | `core:modify_damage` (projectile damage, multiplier: 1.05→1.50) | Multiplier: linear |
| 15 | Quick Reload | Passive | Draw speed reduction (ticks: reduced by 0%→40%) using `core:speed_bonus` | Speed: linear |
| 25 | Piercing Shot | Active (Sneak+Left-click with bow drawn) | Arrow passes through 1→3 targets (modify arrow metadata) | Pierce count: milestone |
| 50 | Mark Target | Passive | Arrow hits apply `core:apply_status` (slowness 1→3 for 2s→5s) | Duration/amp: linear |
| 75 | Flame Arrow | Passive | Arrow ignites target (fire duration: 2s→5s, 100% chance) | Fire duration: linear |
| 100 | Eagle Eye | Passive | Critical hit on targets >15 blocks (multiplier: 2.0→3.5, indicator sound/particle) | Crit mult: linear |

Triggers: `entity_damage` with projectile filter, `shoot_bow` trigger (note: `EntityShootBowEvent` — may need a new trigger).

**Framework note — draw speed:** Paper API has `PlayerItemCooldownEvent` and item cooldowns. Draw speed reduction can be implemented via `Attribute.GENERIC_ATTACK_SPEED` modification when holding a bow, or via a listener on `PlayerStartItemUseEvent` + `PlayerItemCooldownEvent`. If no existing mechanic handles this cleanly, a new `core:bow_draw_speed` mechanic may be needed.

### Step 3: Create YAML files

Each skill YAML follows the same schema as `template-skill.yml`. File structure:

```yaml
# src/main/resources/skills/woodcutting.yml
id: "woodcutting"
display_name: "Woodcutting"
max_level: 100
display:
  icon: "minecraft:iron_axe"
  color: "green"
  style: "segmented"
progression:
  curve: polynomial
  base_xp: 50
  exponent: 2.5
xp_sources:
  - trigger: block_break
    filters:
      - target: "#minecraft:logs"
    reward:
      base: 5
      type: linear
      step: 0.5
abilities:
  - id: "lumberjack"
    display_name: "Lumberjack"
    unlock_level: 1
    requirements: { }
    mechanics:
      - type: "core:yield_multiplier"
        filters:
          - target: "#minecraft:logs"
        parameters:
          multiplier:
            linear:
              base: 1.0
              step: 0.01
              cap: 2.0
    
  - id: "timber_feller"
    display_name: "Timber Feller"
    unlock_level: 25
    requirements:
      cooldown: 5.0
      state: ["is_sneaking"]
    mechanics:
      - type: "core:chain_break"
        filters:
          - target: "#minecraft:logs"
        parameters:
          chain_limit:
            milestones:
              25: 3
              50: 8
              75: 16
              100: 32
    feedback:
      on_activate:
        action_bar: "<green>Timber Feller <white>activated!"
        sounds:
          - type: "minecraft:entity_zombie_break_wooden_door"
            pitch: 1.5
# ... remaining abilities
```

### Step 4: Update `Skilling.java` to save bundled skills on first run

Current `onEnable()` saves template files:

```java
saveResource("template-skill.yml", false);
```

Add a loop to save all bundled skills:

```java
// Save bundled skill configurations on first run
String[] bundledSkills = {"mining.yml", "woodcutting.yml", "excavation.yml", 
                          "farming.yml", "fishing.yml", "archery.yml"};
for (String skill : bundledSkills) {
    saveResource("skills/" + skill, false);
}
```

Also save `template-skill.yml` independently for reference.

### Step 5: New mechanic/trigger needs assessment

| Skill | Required New Mechanic | Required New Trigger |
|-------|----------------------|---------------------|
| Mining | None (all exist) | None |
| Woodcutting | None | None |
| Excavation | None | None |
| Farming | `core:auto_replant` (L100) | None |
| Fishing | None | None |
| Archery | `core:bow_draw_speed` (L15) | `shoot_bow` (`EntityShootBowEvent`) |

**Implement `core:auto_replant`:**
- Hook `BlockBreakEvent` where the broken block is a mature crop
- Check cooldown, state (is_sneaking optional)
- Cancel the natural break, manually set block to its seed-stage equivalent
- Drop items via standard `Block.dropNaturally()`

**Implement `shoot_bow` trigger:**
- Hook `EntityShootBowEvent`
- Verify `entity` is a `Player`
- Return event class: `EntityShootBowEvent.class`

**Implement `core:bow_draw_speed`:**
- Hook `PlayerItemCooldownEvent` or modify `GenericAttackSpeed` attribute when bow is selected
- Alternative: Use existing `core:speed_bonus` with careful item slot matching

### Step 6: Test all skills end-to-end

1. Start Paper dev server with bundled skills
2. Verify all 6 skills appear in `/skills` menu
3. Verify XP gains from each XP source trigger
4. Test each ability — check activation, requirements, cooldowns
5. Verify milestone sub-scaling produces correct values at different levels (use `/skills setlevel`)
6. Test L100 capstone behavior
7. Verify `/skills reload` preserves all skill configs

## Testing

- **Parse test:** `SkillManager` must successfully parse all 6 YAML files without errors. Add `SkillManagerTest` cases for each skill YAML.
- **Integration test:** Start Paper server with all skills, execute each trigger, verify XP gains and ability activations
- **Milestone test:** At each milestone level (L15, L25, L50, L75, L100), verify ability parameters match expected values
- **Cross-skill interaction:** Ensure Mining geologist doesn't block Woodcutting abilities (regression test)
- **Overflow/edge:** L0 shows no abilities locked, L100 shows all abilities unlocked, >L100 doesn't crash

## Self-Review

- The 6 skills exercise all existing mechanics: `yield_multiplier`, `chain_break`, `xp_bonus`, `auto_smelt`, `speed_bonus`, `block_damage`, `modify_damage`, `apply_status`, `saturation_inject`, `modify_craft_output`, `modify_potion_duration`
- New mechanics (`auto_replant`, `bow_draw_speed`) are scoped and simple — no new frameworks needed
- New trigger (`shoot_bow`) follows the existing `SkillTrigger` pattern
- All designs follow SKILL-DESIGN-FRAMEWORK.md — verified against the 6-step audit checklist
- Skill colors use existing config conventions: green (gathering), red (combat)
- No skill depends on external plugins or PlaceholderAPI — they work on a bare Paper server
- Custom tags needed (`#c:excavatable`, `#c:veinminer_blocks`) should be documented in `tags.yml` template as commented examples

## Future Considerations

- **Phase 2 skills:** Combat Defense (Shields, Heavy Armor), Crafting (Smithing, Cooking), Arcane (Alchemy, Enchanting)
- **Phase 3 skills:** Mobility (Riding, Acrobatics), remaining Combat Offense (Heavy Weapons, Light Weapons, Unarmed)
- **Web GUI integration:** The skill editor should be tested with all 6 pre-built skills to verify editing, saving, and reloading
- **Multi-version compatibility:** If Paper API changes in a future version, some triggers/mechanics may need updating — the skill YAMLs themselves should remain version-agnostic
