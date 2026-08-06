# Vanilla+ Skill Engine Design Rule Set

## 1. Core Architectural Pillars

### Pillar I: Non-Destructive Integrity (The "Clean Unplug" Rule)

* **Zero World Corruption:** No ability may permanently alter world geometry. No ability may place non-vanilla blocks. No ability may write custom tile entity data that depends on the plugin to exist.* **Vanilla Container Safety:** Never alter container sizes, slot layouts, or inventory interfaces via custom GUIs. Storage utilities must use standard vanilla containers or temporary virtual windows (e.g., opening a portable crafting table/Ender chest GUI directly) to prevent item loss if the plugin is uninstalled.
* **Native Event Execution:** World-modifying mechanics (e.g., vein mining or tree felling) must run as standard player `BlockBreakEvent` passes, respecting normal tool durability, enchantments, and drop tables.

### Pillar II: Aesthetic & Mechanical Restraint

* **Native Particle & Sound Palette:** No high-fantasy particle tornadoes or custom spell bars. Visual and auditory feedback must use native assets (*Enchanting glyphs*, *Totem bursts*, *Amethyst chimes*, *Action-bar cues*).
* **Subtle System Mechanics:** Arcane or utility mechanics must operate through grounded Minecraft triggers (*status effects*, *knockback vectors*, *item consumption*) rather than modded spellcasting.

### Pillar III: Item Economy Preservation (Vanilla Synergists)

* **Amplify, Never Replace:** Skills must **never** render vanilla items, gear, or structures obsolete. Abilities must require, consume, or enhance existing items rather than offering "free magic" alternatives.
* **Friction Elimination:** High-tier perks should remove mechanical friction from existing items (e.g., allowing Totems to trigger from inventory, removing Ender Pearl self-damage) to elevate vanilla gameplay without bypassing its core risk/reward loops.

### Pillar IV: PvE-First Balance Target

* **Survival Utility:** Every ability must provide direct, tangible value against mobs, the environment, or resource loops.
* **PvP Treatment:** PvP mechanics (e.g., shield disabling, player knockback) must remain passive secondary bonuses and never serve as the primary justification for an ability's existence.

### Pillar V: Event-Driven Engine (Paper API Native Strictness)

* **Zero Constant Ticking Tasks:** Avoid `BukkitRunnable` tasks that run every tick to check player surroundings, scan chunks, or update block states.
* **Player-Centric Effects:** Modify *Player* or *Target Entity* properties directly (status effects, standard damage source calls, held item durability). Do not hack NMS tile entities (e.g., modifying Beacon block tick ranges) or spam block-outline packets.
* **Clean Event Listeners:** All abilities must execute strictly inside standard Paper API event listeners (`BlockBreakEvent`, `EntityDamageByEntityEvent`, `PlayerInteractEvent`, `GenericGameEvent`) running in $O(1)$ time.

---

## 2. Dual-Layer Progression & Mathematical Curves

Progression across Levels 1 to 100 is split into two distinct scalar layers to maintain continuous sense of growth:

### A. The Primary Scalar (Core Linear Engine)

* **Role:** The primary driver of the skill's identity (e.g., *Mining Double Drop Chance*, *Farming Bountiful Harvest*).
* **Formula:** Scales linearly across the entire range ($L_1 \rightarrow L_{100}$):

$$\text{Value} = (\text{PlayerLevel}) \times \text{PrimaryStep}$$


* **Weight:** Serves as the single largest numerical benefit of the skill.

### B. Secondary Scalars (Milestone Sub-Scaling)

* **Role:** Every milestone ability (L15, L25, L50, L75, L100) has its own secondary formula. This formula grows in potency from its unlock point up to Level 100.
* **No Static Milestones:** No ability unlock remains mathematically binary. Parameters like cooldowns, durations, radii, or proc chances grow as the player levels past the unlock threshold.
* **Deliberate Exception (Persistent Unlocks):** A persistent unlock mechanic
  (`core:unlock_recipe`, or any addon `UnlockMechanic`) grants binary permanent
  state — a recipe is unlocked or it is not — so there is nothing to sub-scale.
  These are the sanctioned exception to the no-static-milestones rule. Author
  them as one-time milestone abilities bound to `trigger: level_up`, and keep
  them silent (no action-bar/chat spam) since the grant itself (e.g. the
  recipe-book toast) is the feedback. The engine reconciles them on join and
  after reload so players already past the milestone are caught up.
* **Standard Evaluation Formulas for Configs:**
* **Linear Sub-Growth:**

$$\text{Value} = \text{BaseValue} + (\text{PlayerLevel} - \text{UnlockLevel}) \times \text{StepRate}$$


* **Inverse Cooldown Reduction:**

$$\text{Cooldown} = \text{BaseCD} - \left( \frac{\text{PlayerLevel} - \text{UnlockLevel}}{100 - \text{UnlockLevel}} \right) \times \text{MaxReduction}$$





---

## 3. Milestone Progression Template

Every skill strictly follows a **6-tier milestone progression** spanning **Levels 1 to 100**.

| Milestone | Tier Type | Target Design Role | Example Function |
| --- | --- | --- | --- |
| **Level 1** | **Foundational Passive** | Identity & Linear Engine | Establishes core playstyle + primary scalar + sub-scaling duration/radius. |
| **Level 15** | **Quality-of-Life Passive** | Early Friction Reduction | Low-impact preservation or convenience + sub-scaling proc chance or save rate. |
| **Level 25** | **Primary Ability** | Core Gameplay Hook | Active mechanic (`Shift + Right-Click`) + sub-scaling duration/cooldown reduction. |
| **Level 50** | **Major Passive** | Mid-Game Efficiency Spike | Significant power/yield enhancement + sub-scaling magnitude or effect duration. |
| **Level 75** | **Synergy Passive** | Cross-System Vanilla Link | Enhances an existing vanilla item/system (*Totems*, *Trims*, *Beacons*) + sub-scaling bonus multiplier. |
| **Level 100** | **Mastery Capstone** | High-Level Play Transformer | Game-changing active or passive perk + sub-scaling radius or cooldown. |

---

## 4. Input & Trigger Standard

Active abilities strictly use native vanilla player inputs to prevent client-mod dependencies or keybind clutter:

1. **Tool / Catalyst Stance (`Shift + Right-Click` with Item):** Prepares or executes a timed active buff or instantaneous action.
2. **Directional Intersect (`Sneak + Left-Click` on Block/Target):** Triggers contextual block or target interactions.
3. **Mobility Vector (`Sneak + Jump` or `Double-Crouch`):** Triggers position- or fall-based abilities.

---

## 5. The 6-Step Ability Audit Checklist

Before any ability is added or coded into a YAML skill profile, it must pass all six checks:

1. **Uninstall Test:** If the plugin is removed mid-session, does the world state remain valid and player items stay completely safe? **[YES]**
2. **Vanilla Synergist Test:** Does this ability enhance or require an existing vanilla item/block rather than replacing it for free? **[YES]**
3. **Restraint Test:** Does the visual and auditory feedback look and feel native to mainline Minecraft? **[YES]**
4. **PvE Test:** Does this ability solve a genuine survival problem without relying on player-versus-player combat to be useful? **[YES]**
5. **API Feasibility Test:** Can this ability be implemented using standard Paper API event listeners without per-tick runnables, NMS hacks, or packet spam? **[YES]**
6. **Sub-Scaling Test:** Does this milestone expose at least one dynamic parameter that grows in potency between its unlock level and Level 100? **[YES]**
7. **Lore Clarity Test:** Does every ability with a `requirements:` block surface its costs and conditions as `&7Costs ...` and `&8Requires ...` lore lines, kept in sync with the YAML? **[YES]**