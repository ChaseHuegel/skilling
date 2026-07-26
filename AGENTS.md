# AI Agent Instructions for Skilling

Welcome to the Skilling repository. This file provides architectural context, coding constraints, and design philosophies. **Read these instructions carefully before writing or modifying any code.**

## Project Context
Skilling is a high-performance, data-driven RPG skills engine for PaperMC (Minecraft). It acts as a rules engine, not a traditional plugin.
* **The Golden Rule:** There are ZERO hardcoded skills, levels, or abilities in the Java backend.
* All mechanics, triggers, and evaluators are decoupled modules.
* Content is constructed entirely via YAML configurations by the end-user.
* The system is designed to maintain 20 TPS under heavy load.

## Tech Stack
* **Target API:** Paper API (Latest)
* **Language:** Java 21 (LTS) - *Use modern features: Records, Switch Expressions, Pattern Matching.*
* **Build System:** Gradle (Kotlin DSL)
* **Database:** Embedded SQLite (WAL mode) with HikariCP pooling.

---

## Architectural Rules

### 1. Composition Over Inheritance (ECS-Style)
Do not create classes like `MiningSkill` or `WoodcuttingAbility`. Instead, build reusable components:
* **Triggers:** Listeners that hook into Spigot events.
* **Mechanics:** Executable actions (e.g., `YieldMultiplierMechanic`, `ApplyStatusMechanic`).
* **Parameter Evaluators:** Classes that take `(currentLevel, unlockLevel)` and return a `double`.
* All new Mechanics and Evaluators must be registered in their respective `Registry` singletons during `onEnable()`.

### 2. Thread Safety & Database I/O
* **Never block the Bukkit Main Thread.**
* All SQLite database reads/writes must be executed asynchronously.
* **State Management:** Use the Write-Behind Cache pattern. Update the `PlayerProfile` in-memory `ConcurrentHashMap`, flag it as `isDirty = true`, and let the async batch worker handle the SQL `UPSERT`.
* Keep SQLite in `PRAGMA journal_mode=WAL;` to prevent file contention.

### 3. The Requirements Engine
Ability execution must follow the **Check, Execute, Consume** pattern:
1. `requirements.check(player)`: Evaluate conditions (cooldowns, items, states). Returns a `RequirementResult` object. Do NOT return raw booleans.
2. `mechanic.execute(...)`: Run the logic if the check passes.
3. `requirements.consume(player)`: Deduct items and apply cooldowns only after successful execution.

### 4. UI & Inventory Security
* **Lazy Instantiation:** Build Bukkit `Inventory` objects on-demand and cache them in the `PlayerProfile`. Invalidate the cache entirely when a player's level changes.
* **Dynamic Lore:** Use `LoreResolver` to inject live math from `ParameterEvaluator` outputs into strings. Never hardcode `{placeholder}` values.
* **Anti-Dupe (Poison Pill):** Every UI `ItemStack` must be tagged with a hidden byte via Paper's `PersistentDataContainer`. The global inventory listener must `setCancelled(true)` on all clicks/drags in custom holders and vaporize any tagged item found outside the UI.

### 5. Configs & Tags
* When writing block or item filters, support Vanilla namespaces (e.g., `#minecraft:logs`).
* Always route tag checks through the custom `TagResolver` to support user-defined custom tags in `tags.yml`.
* Flatten tag resolution into `EnumSet<Material>` or `EnumSet<EntityType>` during plugin load to keep event listener lookups at O(1) complexity.

---

## Coding Style & Conventions
* **Fail-Fast:** Throw `IllegalArgumentException` during YAML parsing if a config is malformed. Do not let bad configs silently fail at runtime.
* **Performance:** Avoid regex compilation inside loops or high-frequency events.
* **Debouncing:** When providing failure feedback (e.g., playing a dud sound for an ability on cooldown), route it through the `FeedbackDebouncer` to prevent client-side spam.
* **Component API:** Use Paper's modern Component API for items and text. Avoid legacy `&` color code translations where MiniMessage or Components are applicable.