# Technical Requirements Specification: Vanilla+ Skills Engine

## 1. Environment & Tech Stack

The plugin is a rules engine built exclusively for the Paper server ecosystem.

| Component | Requirement |
| --- | --- |
| **Target API** | Paper API (Latest Release) |
| **Language** | Java 21 (LTS) |
| **Build System** | Gradle (Kotlin DSL) |
| **Database** | SQLite (Local, file-based) |
| **Connection Pool** | HikariCP (Shaded into the plugin jar) |
| **Command Framework** | Incendo Cloud (cloud-paper, cloud-annotations) |
| **Config Format** | YAML (Bukkit native `YamlConfiguration`) |

---

## 2. Core Architectural Patterns

The system treats skills as data. It uses composition instead of inheritance to avoid hardcoded logic.

* **Registry Pattern:** All triggers, mechanics, and parameter evaluators must be registered in central singleton managers during plugin initialization (`onEnable`).
* **Decoupled State:** Gameplay logic must operate entirely in-memory. The Bukkit main thread is strictly forbidden from executing blocking I/O database queries.
* **Result Object Pattern:** Requirement checks must return a unified `RequirementResult` object containing the boolean success state, failure reason, and dynamic string placeholders for feedback.
* **API-First Design:** The plugin must expose a `Service` via the Bukkit `ServicesManager` to allow addon plugins to register custom mechanics and evaluators.

### Hardcoded Gameplay Tables (Design Decision, ISSUE-301)

The golden rule forbids hardcoded skills, levels, and abilities. Small gameplay-data
tables inside mechanic implementations are a separate boundary. The decision is:

**Keep them immutable in Java; never expose them in YAML.**

These tables are one of four kinds, none of which is author-facing content:

| Table | Kind | Why immutable |
|---|---|---|
| `AutoSmeltMechanic.SMELT_MAP` | Vanilla mirror | Repeats Minecraft's fixed smelting recipes; exposing them would fork the game's own data |
| `OffhandStrikeMechanic.BASE_DAMAGE` | Vanilla mirror | Mirrors the fixed vanilla weapon attack-damage values |
| `AutoReplantMechanic` crop list | Capability boundary | Defines which blocks a mechanic can act on |
| `PotionEffectResolver` / `ModifyAttributeMechanic` legacy numeric IDs | Compatibility shim | Deprecated numeric-ID mapping for old configs |
| `SkillEventListener.projectileToMaterial` | Engine plumbing | Maps projectile entity types to a filter material for the `target` filter |

Each table carries an explicit "immutable" note in its Javadoc. If a future table
encodes author-tunable tuning data (rewards, chances, limits), it belongs in YAML
as an evaluator parameter or filter, not in Java.

---

## 3. Data Persistence & Lifecycle

Database operations must maintain 20 TPS (Ticks Per Second) under heavy concurrent load.

### Database Schema

* **Mode:** SQLite must be initialized using Write-Ahead Logging (`PRAGMA journal_mode=WAL;`).
* **Structure:** Normalized composite keys. Store only raw `xp` integers in the database. Effective levels are derived purely in-memory.

### The Asynchronous Pipeline

* **Hydration:** Player data is fetched asynchronously during `AsyncPlayerPreLoginEvent`. The player is only allowed to spawn once hydration completes.
* **Write-Behind Cache:** XP gains flag a player's `PlayerProfile` as `isDirty = true`.
* **Batch Saving:** An async Bukkit scheduler runs every 3-5 minutes, draining the dirty cache and executing a batched `UPSERT` using a pooled HikariCP connection.
* **Graceful Shutdown:** On `onDisable()`, the plugin must force one final synchronous flush of the dirty cache to prevent data loss.

---

## 4. Configuration & Resolution Engine

The engine acts as a parser mapping YAML definitions to executable Java interfaces.

* **Plugin Config:** A global `config.yml` must be generated on first run, governing database pool size, boss bar pool capacity (default: 2), and debounce intervals (default: 500ms).
* **Parameter Evaluators:** All numeric configurations must be parsed into polymorphic evaluators (`LinearEvaluator`, `MilestoneEvaluator`, `ConstantEvaluator`) that dynamically calculate outputs based on a player's effective level.
* **Vanilla Tag Resolution:** String filters beginning with `#` must query the Bukkit `Tag` API.
* **Custom Tag Registry:** The engine must parse the `tags/` data folder (generated on first run as `tags/base.yml`) to support custom item/block groupings before falling back to vanilla namespaces. The folder must be scanned recursively. Duplicate tag keys must merge additively. A file that cannot be read as tags must log a warning and be skipped. The format must support both raw material lists and cross-references to vanilla `#` tags.
* **Ability Registry:** The engine must parse the optional `abilities/` data folder into a registry keyed by ability id. A skill may reference an id and override individual fields on top of the registered base definition.
* **O(1) Execution:** Tags and filters must be flattened into `EnumSet<Material>` or `EnumSet<EntityType>` during plugin load to guarantee fast event routing.

---

## 5. UI/UX & Security Requirements

User interfaces must be dynamically generated and secured against network race conditions.

### Hierarchical GUIs

* **Lazy Instantiation:** Chest menus are generated on demand and cached inside the `PlayerProfile`.
* **Cache Invalidation:** The GUI cache is entirely cleared the moment a player crosses a level threshold, forcing a rebuild on the next menu open.
* **Lore Injection:** UI text must parse `{placeholder}` strings, query the corresponding `ParameterEvaluator`, and format the output dynamically.
* **Item State:** Skill levels map to the `ItemStack` amount using Paper's component API to bypass the legacy 64-item stack limit.

### Inventory Security

* **Strict Routing:** All inventory events (`InventoryClickEvent`, `InventoryDragEvent`) within custom holders must explicitly deny shift-clicks, number-key swaps, and off-hand swaps.
* **Poison Pill Verification:** Every UI item must be tagged with a hidden byte via Paper's `PersistentDataContainer`. A global listener must intercept and delete any tagged item that exists outside a controlled UI context.

### Real-Time Feedback

* **LRU Boss Bar Cache:** Player Boss Bars must be managed via a Least Recently Used map (default size: 2).
* **Debouncer:** Failure feedback (e.g., ability on cooldown) must be throttled using a 500ms timestamp cache per player to prevent audio/visual spam.

---

## 6. Command & Administration Layer

Administrative commands must execute safely without corrupting the async data pipeline.

* **Subcommand Routing:** Use Incendo Cloud's annotation-driven command builder with `cloud-paper` for argument casting and permission node routing.
* **Single Command Tree:** All functionality lives under `/skills`. There is no separate `/skillsadmin` root. A bare `/skills` opens the player's skill overview UI.
* **Dynamic Tab Completion:** Skill ID arguments must auto-complete by querying the live `SkillRegistry`, ensuring custom YAML skills appear instantly.
* **Offline Player Handling:** Admin commands targeting offline players must execute directly against the database. They must flag the row to trigger UI fanfare upon their next login.
* **Deterministic Reloads:** The `/skills reload` command must follow a strict lockdown sequence. The sequence is: freeze interactions, close active GUIs, flush the database, rebuild registries, invalidate UI caches, and unlock interactions.

---

## 7. Testing Requirements

| Requirement | Details |
|---|---|
| **Framework** | JUnit 5 with `./gradlew test` invocation |
| **Coverage targets** | All `ParameterEvaluator` implementations, `RequirementEngine` lifecycle, `TagResolver` resolution, `LoreResolver` placeholder injection |
| **Validation gate** | `./gradlew test` must pass alongside `./gradlew build` at the end of each development phase |
| **Test location** | `../../src/test/java/io/github/chasehuegel/skilling` mirroring the main source tree |