# Architecture & Design Specification: Data-Driven Skills Engine

## 1. Core Philosophy & Engine Architecture

The plugin operates as a decoupled rules engine. The Java backend contains zero hardcoded skills, levels, or abilities. Instead, it provides a library of **Triggers**, **Mechanics**, and **Evaluators**. The YAML configuration files act as the designers, wiring these components together into playable content.

### The Registry Pattern

All logic is abstracted into independent, reusable modules registered during plugin initialization. Addon developers can inject custom modules into these registries via an exposed API.

* **Triggers:** Hooks into Spigot/Paper events (e.g., `block_break`, `entity_damage`).
* **Mechanics (Effects):** The executable logic (e.g., `yield_multiplier`, `apply_status`, `chain_break`).
* **Filters (Tags):** Conditional gates leveraging vanilla and custom namespaces (e.g., `#minecraft:logs`, `is_sneaking`).
* **Parameter Evaluators:** Polymorphic math processors that calculate dynamic values based on the player's current level versus the ability's unlock level. Registered under kebab-case keys (`linear`, `milestone`, `constant`, `random`, `polynomial`).

### Package / Module Structure

All code lives under `io.github.chasehuegel.skilling`. The module layout is:

```
io.github.chasehuegel.skilling
  Skilling.java               # Main class (extends JavaPlugin)
  api/
    SkillingAPI.java          # Bukkit ServicesManager API for addons
    Registries.java           # Container for MechanicRegistry, TriggerRegistry, EvaluatorRegistry
  engine/
    SkillManager.java         # Parses YAML → SkillDefinition records
    registry/
      MechanicRegistry.java   # Map<String, Supplier<SkillMechanic>>
      TriggerRegistry.java    # Map<String, EventExecutor>
      EvaluatorRegistry.java  # Map<String, ParameterEvaluator>
    profile/
      PlayerProfile.java      # In-memory player state, isDirty flag
      ProfileManager.java     # ConcurrentHashMap<UUID, PlayerProfile>
    db/
      DatabaseManager.java    # HikariCP pool, WAL init, schema creation
      AsyncBatchWorker.java   # Scheduled async UPSERT draining
    requirements/
      RequirementResult.java  # Record: success, FailureReason, placeholders
      RequirementEngine.java  # Check → Execute → Consume pipeline
    mechanic/
      SkillMechanic.java      # Interface: execute(Player, int, int, Event)
      impl/                   # YieldMultiplierMechanic, ChainBreakMechanic, etc.
    trigger/
      SkillTrigger.java       # Interface binding events to YAML trigger IDs
      impl/                   # BlockBreakTrigger, EntityDamageTrigger, etc.
    evaluator/
      ParameterEvaluator.java # Interface: evaluate(int currentLevel, int unlockLevel) → double
      impl/                   # LinearEvaluator, MilestoneEvaluator, ConstantEvaluator, PolynomialEvaluator
    tag/
      TagResolver.java        # Resolves #minecraft: and #c: tags into EnumSet
      CustomTagLoader.java    # Reads tags.yml
    ui/
      SkillMenuBuilder.java   # Lazy-builds Inventory from SkillDefinition
      LoreResolver.java       # Injects {placeholder} → evaluator output
      UIProtectionListener.java # Anti-dupe, shift-click denial, poison pill
      PoisonPillTag.java      # NamespacedKey constant for PDC
    feedback/
      FeedbackDebouncer.java  # 500ms throttle per player-ability key
      BossBarPool.java        # LRU LinkedHashMap with configurable capacity
      FanfareDispatcher.java  # Particles, sounds, action bar on level-up/activate
    command/
      SkillsCommand.java      # /skills command tree (player + admin subcommands)
```

## 2. Configuration Templates

The plugin ships with two default config files generated on first run.

### config.yml (Global Settings)

```yaml
database:
  pool_size: 10
  wal_mode: true
bossbar:
  max_active: 2
  fade_ticks: 40
debouncer:
  interval_ms: 500
```

### tags.yml (Custom Tag Definitions)

```yaml
custom_tags:
  c:ores:
    - "minecraft:coal_ore"
    - "minecraft:iron_ore"
    - "minecraft:gold_ore"
    - "#minecraft:copper_ores"    # vanilla tags can be cross-referenced
  c:logs:
    - "#minecraft:logs"
  c:gems:
    - "minecraft:diamond"
    - "minecraft:emerald"
```

## 3. State Management & Data Persistence

To maintain maximum server tick rates under heavy I/O loads, the system decouples gameplay state from the database using asynchronous batching.

### In-Memory State

* **Hydration:** Player data is fetched via `AsyncPlayerPreLoginEvent` and cached in a `ProfileManager`.
* **Write-Behind Cache:** XP gains update the in-memory `ConcurrentHashMap` and flag the profile as `isDirty = true`.
* **Uncapped Progression:** The database stores raw XP integers. Effective levels are calculated dynamically in memory via the configured polynomial curve, enforcing the YAML's `max_level` ceiling without destroying overflow XP.

### SQLite Implementation

* **WAL Mode:** Write-Ahead Logging allows concurrent reads alongside a single asynchronous writer.
* **Event Queue:** A scheduled async Bukkit task periodically drains the dirty cache and executes a batched `UPSERT` transaction, ensuring zero main-thread blocking.

## 4. The Execution Pipeline

When a player performs an action, the engine processes it through a strict, deterministic pipeline.

### The Requirements Engine

Before any Mechanic fires, a pre-execution gate evaluates a list of configured requirements.

* **Result Object Pattern:** Evaluators return a structured object containing success state, failure type, and dynamic placeholders (e.g., remaining cooldown time).
* **Check vs. Consume:** Requirements are strictly checked first. If all pass, the mechanics execute, and then the requirements (items, cooldowns) are consumed.
* **Debouncer:** Failure feedback (dud sounds, action bar warnings) is throttled via a 500ms timestamp cache to prevent client-side spam.

### The Feedback Engine

Post-execution, the engine parses the YAML `feedback` node to dispatch visual and auditory rewards.

* **Level Ups:** Triggered by a central dispatcher checking XP thresholds. Broadcasts a Title to the player and a permanent log to the chat. Milestone levels dynamically append unlocked ability names.
* **Ability Activations:** Dispatches configured particles, sounds, and action bar text targeted at the player or the affected entity.

## 5. User Interface Architecture

The UI is dynamically generated from the YAML files and heavily protected against client-server desyncs.

### Hierarchical Chest GUIs

* **Lazy Instantiation:** Menus are built once upon request and cached in the player's session. The cache is immediately invalidated and rebuilt when the player gains a level.
* **Dynamic Lore Injection:** The UI generator parses string placeholders (e.g., `{chain_limit}`) and runs them through the Parameter Evaluators to display exact, real-time math based on the player's current level.
* **Icon State:** Icons utilize custom model data. The `ItemStack` amount dynamically reflects the player's exact level (1-100) utilizing Paper's max-stack-size component.
* **Security (Double Defense):** Strict inventory event routing denies all shift-clicks, number-key swaps, and offhand swaps. A `PersistentDataContainer` byte-tag acts as a poison pill, vaporizing any UI item that accidentally glitches into the game world.

### Real-Time UX (Boss Bars)

* **LRU Pool:** A Least Recently Used cache limits the screen to a configurable maximum of active Boss Bars (default: 2).
* **Global Ticker:** A single 1-tick repeating task iterates through online players, decrementing the Time-To-Live (TTL) on active bars and fading them out to prevent object instantiation bloat.

---

## 6. Command & Administration

All commands use Incendo Cloud for registration, argument parsing, and permission routing under a single `/skills` root.

| Command | Permission | Description |
|---|---|---|
| `/skills` | — | Opens the player's skill overview UI (lazy-built from YAML configs) |
| `/skills progress [skill]` | — | Shows current level, XP, and next-level progress in chat |
| `/skills reload` | `skilling.admin` | Full lockdown reload (see below) |
| `/skills setlevel <player> <skill> <level>` | `skilling.admin` | Override a player's level |
| `/skills addxp <player> <skill> <amount>` | `skilling.admin` | Grant XP to a player |
| `/skills reset <player> [skill]` | `skilling.admin` | Reset a player entirely or per skill |

### Reload Lockdown Sequence

The `/skills reload` command follows a deterministic six-phase sequence:

1. **Freeze** — Set an atomic `reloading` flag. All event listeners check this flag and short-circuit interactions.
2. **Close GUIs** — Force-close all open skill menus for online players.
3. **Flush DB** — Synchronously drain the dirty profile cache to SQLite.
4. **Rebuild Registries** — Clear and re-parse all YAML skill definitions, evaluators, and tag maps.
5. **Invalidate Caches** — Clear all `PlayerProfile` UI inventory caches.
6. **Unlock** — Clear the `reloading` flag to resume normal operation.

### Offline Player Targeting

Admin commands operating on offline players execute directly against SQLite. The affected row is flagged (`fanfare_pending = 1`) so the next time that player logs in, the login listener triggers the appropriate level-up fanfare.

---

## 7. Testing Strategy

All non-Bukkit logic must be covered by JUnit 5 unit tests.

| Component | What to Test |
|---|---|
| `ParameterEvaluator` implementations | Boundary values, edge cases (level=0, level=max), min/max clamping |
| `RequirementEngine` | Check returns `RequirementResult` on pass/fail; Consume deducts items correctly; Consume fails when inventory lacks items |
| `TagResolver` | Vanilla `#minecraft:` resolution, custom `#c:` tag resolution, fallback behavior |
| `LoreResolver` | `{placeholder}` injection matches evaluator output, missing placeholder handling |
| `ConfigurationParser` / `SkillManager` | Malformed YAML throws `IllegalArgumentException`, well-formed YAML produces correct `SkillDefinition` tree |

Tests run via `./gradlew test` and must pass before each phase is considered complete.

---

## 8. Documentation Structure

The project ships with user-facing markdown documentation in `docs/`. These files are written for server owners and addon developers, not the plugin's implementors.

| Document | Audience | Content |
|---|---|---|
| `docs/getting-started.md` | Server owners | Installation, first run, basic `/skills` usage |
| `docs/configuration.md` | Server owners | Reference for `config.yml` and `tags.yml` with all supported keys |
| `docs/creating-skills.md` | Server owners / designers | Full YAML schema for skill definitions, abilities, XP sources, requirements, with annotated examples |
| `docs/api-integration.md` | Addon developers | How to register custom mechanics, triggers, evaluators via `SkillingAPI`. Maven/Gradle coordinates, code samples |
| `docs/capabilities.md` | Addon developers | Catalog of every built-in mechanic, trigger, and evaluator with their parameters and YAML usage |

All YAML config templates (`config.yml`, `tags.yml`, `template-skill.yml`) must contain inline commented documentation for each supported key, including valid values, defaults, and short descriptions.

---

## 9. Vanilla+ Content Blueprint (The Configurations)

The engine will ship with default YAML configurations mapping out a 32-skill web. These designs serve as the template for utilizing the engine's default mechanics.

### Harvesting & Gathering (Green, Segmented Boss Bars)

Focuses on resource generation and block-state manipulation.

* **Mining, Woodcutting, Digging:** Utilizes `yield_multiplier` and `chain_break` mechanics. Tags distinguish ores from logs and dirt.
* **Farming, Herbalism:** Hooks into crop growth ticks, right-click replanting, and localized flora generation.
* **Husbandry:** Modifies mob breeding chances and taming success rates.

### Combat Offense (Red, Segmented Boss Bars)

Focuses on dynamic entity damage manipulation and attribute modifiers.

* **Heavy Weapons, Light Weapons, Unarmed:** Utilizes `modify_damage` (armor piercing, backstabs) and applies temporary status effects (slowness, bleed).
* **Archery, Throwing:** Modifies projectile velocity, gravity, and item return mechanics.
* **One Handed, Dual Wield:** Checks equipment slot states to grant dynamic `generic.attack_speed` and AoE sweep particles.

### Combat Defense (Blue, Solid Boss Bars)

Focuses on survivability, avoidance, and kinetic mitigation.

* **Shields, Heavy Armor:** Utilizes knockback resistance modifiers, directional damage blocking, and entity repulsion.
* **Light Armor, Medium Armor, Unarmored:** Utilizes `cancel_damage` (evasion), permanent step-assist attributes, and temporary speed buffs.

### Crafting & Trade (Yellow, Segmented Boss Bars)

Focuses on inventory manipulation and block metadata.

* **Smithing, Carpentry, Masonry, Tailoring:** Hooks into `CraftItemEvent` and `FurnaceExtractEvent` to multiply outputs and inject NBT tags (e.g., bonus durability).
* **Building:** Utilizes mid-air block placement vectors and structural blast-resistance injection.
* **Cooking:** Injects dynamic saturation values into crafted foods and allows potion-effect infusion.

### Arcane & Support (Purple, Segmented Boss Bars)

Focuses on environmental control and area-of-effect buffs.

* **Alchemy, Enchanting:** Modifies brew times, potion duration tags, and XP/Lapis costs.
* **Piety, Bard:** Utilizes AoE status effect clouds, neutral mob aggro cancellation, and Jukebox/Note Block interaction tracking.
* **Wizardry:** Consumes player XP to cast custom projectile vectors and short-range teleports.

### Mobility & Exploration (White, Solid Boss Bars)

Focuses on movement tech and mount manipulation.

* **Riding:** Modifies `GENERIC_MOVEMENT_SPEED` on vehicles, grants fall-damage immunity to mounts, and applies mounted-combat multipliers.
* **Acrobatics:** Intercepts kinetic/fall damage events based on timing windows and reduces sprinting/jumping hunger exhaustion.