# AI Agent Instructions for Skilling Engine Core (src)

This is the Java plugin backend of Skilling — the data-driven rules engine. It is the closest DOX contract for all work under `src/`. Read the root `AGENTS.md` for project-wide rules, then use this file for local engine rules.

## Purpose

Implement the PaperMC rules engine with **zero hardcoded skills, levels, or abilities**. Gameplay content is constructed entirely from YAML by end-users. The backend provides the reusable library of Triggers, Mechanics, Filters, and Parameter Evaluators that the configs wire together.

## Ownership

- `src/main/java/io/github/chasehuegel/skilling/engine/**` — core engine (parsing, registries, profiles, db, requirements, mechanics, triggers, evaluators, tags, ui, feedback, command, lockdown, listeners, events, integration).
- `src/main/java/io/github/chasehuegel/skilling/api/**` — main plugin-side API impl/registries (the public API interfaces themselves are owned by `skilling-api/AGENTS.md`).
- `src/main/resources/**` — `plugin.yml`/`paper-plugin.yml`, default `config.yml`, `tags.yml`, and bundled skill YAML.
- `src/test/**` — JUnit 5 unit tests.
- **NOT owned:** `io.github.chasehuegel.skilling.web` — the Web GUI backend package is owned by `web/AGENTS.md`.

## Local Contracts

### 1. Composition Over Inheritance (ECS-Style)
Do not create classes like `MiningSkill` or `WoodcuttingAbility`. Build reusable components:
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
* **Plugin Config:** Global settings (`config.yml`) govern database pool size, boss bar pool capacity, and debounce intervals.
* When writing block or item filters, support Vanilla namespaces (e.g., `#minecraft:logs`).
* Always route tag checks through the custom `TagResolver` to support user-defined custom tags in `tags.yml`. Entity-type tags (`entity_tags:` in `tags.yml`, e.g. `#c:undead`) are resolved by the parallel `EntityTagResolver` for the `target_type` state filter.
* Flatten tag resolution into `EnumSet<Material>` or `EnumSet<EntityType>` during plugin load to keep event listener lookups at O(1) complexity.

### 6. Command & Administration
* Use Incendo Cloud for command registration, argument casting, and permission routing.
* All functionality lives under a single `/skills` command tree — no separate `/skillsadmin`.
* A bare `/skills` (no arguments) opens the player's skill overview UI.
* Skill IDs auto-complete by querying the live `SkillRegistry`.
* Admin commands targeting offline players must execute directly against the database and flag the row for fanfare on next login.
* `/skills reload` follows a strict lockdown sequence: freeze interactions, close GUIs, flush DB, rebuild registries, invalidate UI caches, unlock.

### 7. Java Code Documentation
* **Javadoc is required** on all public API methods, interfaces, abstract classes, and non-trivial overrides. Keep it concise: explain *what* and *why*, not *how*.
* **Avoid inline comments that restate the code.** Bad: `x += 1; // increment x by 1`. Good: `x += 1; // shift window start to exclude the just-consumed entry`.
* **Use inline comments only** to explain non-obvious edge cases, performance considerations, or why a seemingly wrong approach was chosen.
* Every `SkillMechanic`, `SkillTrigger`, and `ParameterEvaluator` implementation must have a class-level Javadoc explaining its purpose, YAML key, and required/optional parameters.

### 8. YAML Template Documentation
* Every configurable YAML file shipped under `resources/` (`config.yml`, `tags.yml`, skill definitions, `template-skill.yml`) must include commented documentation for each key: supported values, defaults, and a brief description.
* Include commented-out examples showing configuration possibilities inline in templates.

### 9. Coding Style & Conventions
* **Fail-Fast:** Throw `IllegalArgumentException` during YAML parsing if a config is malformed. Do not let bad configs silently fail at runtime.
* **Performance:** Avoid regex compilation inside loops or high-frequency events.
* **Debouncing:** When providing failure feedback (e.g., playing a dud sound for an ability on cooldown), route it through the `FeedbackDebouncer` to prevent client-side spam.
* **Component API:** Use Paper's modern Component API for items and text. Avoid legacy `&` color code translations where MiniMessage or Components are applicable.

## Work Guidance

* Deep architecture specs live in `docs/dev/DESIGN.md` (module layout, execution pipeline, UI architecture) and `docs/dev/REQUIREMENTS.md` (tech requirements, schema, async pipeline). Read them for design context; this file is the binding contract.
* The content design framework (milestones, scaling curves, vanilla-restraint pillars) lives in `docs/dev/SKILL-DESIGN-FRAMEWORK.md` — apply it when authoring bundled skill YAML.
* The skill YAML schema template is `docs/dev/template-skill.yml`.
* Follow the issue workflow and validation gate in root `AGENTS.md` when resolving `docs/issues/INDEX.md` items.

## Verification

* `./gradlew build` — must pass after every phase.
* `./gradlew test` — JUnit 5; must pass after every phase.
* **What to Test:** Every `ParameterEvaluator` implementation, the `RequirementEngine` check/consume lifecycle, `TagResolver` resolution, and `LoreResolver` placeholder injection must have unit tests.
* Tests live in `src/test/` mirroring the main source tree.
* **Registry bootstrap:** Bukkit's `Registry` static initializer runs once per JVM and fails in a plain-JUnit JVM unless a `RegistryAccess` is present. `src/test/.../testutil/FakeRegistryAccess.java` is installed via `src/test/resources/META-INF/services/` so registry-backed constants (`Attribute.MAX_HEALTH`, `Material.getMaxDurability()`, enchantments) initialize regardless of test ordering. Do not mock `ItemType`; the fake returns `null` for ITEM entries because `Material.getMaxDurability()` treats that as "no durability". Tests that install their own `mockStatic(RegistryAccess.class)` must keep their `getRegistry` answers type-aware (delegate non-owned keys to `FakeRegistryAccess.registryFor(...)`) so a per-test fake cannot poison another registry during class-init.

## Child DOX Index

| Path | Scope |
|---|---|
| `web/AGENTS.md` | The Java backend package `io.github.chasehuegel.skilling.web` physically lives under this tree (`src/main/java/.../web/**`) but is owned by `web/AGENTS.md`; route web-edit work there. |
