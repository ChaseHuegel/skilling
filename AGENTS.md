# AI Agent Instructions for Skilling

Welcome to the Skilling repository. This file provides architectural context, coding constraints, and design philosophies. **Read these instructions carefully before writing or modifying any code.**

Companion files with deeper specifications: `README.md`, `docs/dev/REQUIREMENTS.md`, `docs/dev/DESIGN.md`, `docs/dev/template-skill.yml`, `docs/dev/CONVENTIONS-COMMITS.md` (commit message format), and `web/AGENTS.md` for Web GUI configuration (including Minecraft asset texture version).

## Project Context
Skilling is a high-performance, data-driven RPG skills engine for PaperMC (Minecraft). It acts as a rules engine, not a traditional plugin.
* **The Golden Rule:** There are ZERO hardcoded skills, levels, or abilities in the Java backend.
* All mechanics, triggers, and evaluators are decoupled modules.
* Content is constructed entirely via YAML configurations by the end-user.
* The system is designed to maintain 20 TPS under heavy load.

## Package Base
All code lives under `io.github.chasehuegel.skilling`.

## Tech Stack
* **Target API:** Paper API (Latest release)
* **Language:** Java 21 (LTS) — *Use modern features: Records, Switch Expressions, Pattern Matching.*
* **Build System:** Gradle (Kotlin DSL)
* **Database:** Embedded SQLite (WAL mode) with HikariCP pooling.
* **Command Framework:** Incendo Cloud (with `cloud-paper` and `cloud-annotations`)

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
* **Plugin Config:** Global settings (`config.yml`) govern database pool size, boss bar pool capacity, and debounce intervals.
* When writing block or item filters, support Vanilla namespaces (e.g., `#minecraft:logs`).
* Always route tag checks through the custom `TagResolver` to support user-defined custom tags in `tags.yml`.
* Flatten tag resolution into `EnumSet<Material>` or `EnumSet<EntityType>` during plugin load to keep event listener lookups at O(1) complexity.

### 6. Command & Administration
* Use Incendo Cloud for command registration, argument casting, and permission routing.
* All functionality lives under a single `/skills` command tree — no separate `/skillsadmin`.
* A bare `/skills` (no arguments) opens the player's skill overview UI.
* Skill IDs auto-complete by querying the live `SkillRegistry`.
* Admin commands targeting offline players must execute directly against the database and flag the row for fanfare on next login.
* `/skills reload` follows a strict lockdown sequence: freeze interactions, close GUIs, flush DB, rebuild registries, invalidate UI caches, unlock.

---

## Issue Resolution Workflow

When resolving items from `docs/project/ISSUES.md`, follow this strict sequence:

1. **One issue at a time** — Tackle one issue (including all its sub-bullets) completely before starting the next.
2. **Plan first (if complex)** — For issues with sub-bullets or non-trivial scope, write a brief development plan before writing any code.
3. **Build** — After making changes, run `./gradlew build`. Fix any compiler errors before continuing.
4. **Test** — Run `./gradlew test`. Fix any test failures introduced by the changes.
5. **Self-review** — Read the diff (`git diff`) to verify correctness, style, and adherence to conventions.
6. **Mark complete** — Check off the resolved bullet(s) in `docs/project/ISSUES.md`.
7. **Commit** — `git add -A && git commit -m "..."` with a message following `docs/dev/CONVENTIONS-COMMITS.md`. **All commits MUST adhere to this convention.**
8. **Next issue** — Repeat from step 1 for the next unchecked item.

## Testing & Validation
* **Test Framework:** Use JUnit 5 for unit testing all non-Bukkit logic (evaluators, parsers, requirements engine).
* **What to Test:** Every `ParameterEvaluator` implementation, the `RequirementEngine` check/consume lifecycle, `TagResolver` resolution, and `LoreResolver` placeholder injection must have unit tests.
* **Phase Validation:** After each development phase, run `./gradlew test` in addition to `./gradlew build`. All tests must pass before proceeding.

## Documentation & Self-Documenting Code

### Java Code Documentation
* **Javadoc is required** on all public API methods, interfaces, abstract classes, and non-trivial overrides. Keep it concise: explain *what* and *why*, not *how*.
* **Avoid inline comments that restate the code.** Bad: `x += 1; // increment x by 1`. Good: `x += 1; // shift window start to exclude the just-consumed entry`.
* **Use inline comments only** to explain non-obvious edge cases, performance considerations, or why a seemingly wrong approach was chosen.
* Every `SkillMechanic`, `SkillTrigger`, and `ParameterEvaluator` implementation must have a class-level Javadoc explaining its purpose, YAML key, and required/optional parameters.

### YAML Template Documentation
* Every configurable YAML file (`config.yml`, `tags.yml`, skill definitions) must include commented documentation for each key: supported values, defaults, and a brief description.
* Include commented-out examples showing configuration possibilities inline in templates.

### External Documentation
* A `docs/` directory must exist at project root containing markdown files for end-users and addon developers:
  * `docs/users/getting-started.md` — Installation, first run, basic usage (`/skills` commands).
  * `docs/users/configuration.md` — Reference for `config.yml` and `tags.yml` with all supported keys.
  * `docs/users/creating-skills.md` — Full YAML schema for skill definitions, abilities, XP sources, and requirements, with annotated examples.
  * `docs/users/api-integration.md` — How to register custom mechanics, triggers, and evaluators via the API. Maven/Gradle coordinates, code samples.
  * `docs/users/capabilities.md` — Catalog of every built-in mechanic, trigger, and evaluator with their parameters and YAML usage.
* These docs are **user-facing** and must use clear language free of implementation jargon.

## Coding Style & Conventions
* **Fail-Fast:** Throw `IllegalArgumentException` during YAML parsing if a config is malformed. Do not let bad configs silently fail at runtime.
* **Performance:** Avoid regex compilation inside loops or high-frequency events.
* **Debouncing:** When providing failure feedback (e.g., playing a dud sound for an ability on cooldown), route it through the `FeedbackDebouncer` to prevent client-side spam.
* **Component API:** Use Paper's modern Component API for items and text. Avoid legacy `&` color code translations where MiniMessage or Components are applicable.

# DOX framework

- DOX is highly performant AGENTS.md hierarchy installed here
- Agent must follow DOX instructions across any edits

## Core Contract

- AGENTS.md files are binding work contracts for their subtrees
- Work products, source materials, instructions, records, assets, and durable docs must stay understandable from the nearest applicable AGENTS.md plus every parent AGENTS.md above it

## Read Before Editing

1. Read the root AGENTS.md
2. Identify every file or folder you expect to touch
3. Walk from the repository root to each target path
4. Read every AGENTS.md found along each route
5. If a parent AGENTS.md lists a child AGENTS.md whose scope contains the path, read that child and continue from there
6. Use the nearest AGENTS.md as the local contract and parent docs for repo-wide rules
7. If docs conflict, the closer doc controls local work details, but no child doc may weaken DOX

Do not rely on memory. Re-read the applicable DOX chain in the current session before editing.

## Update After Editing

Every meaningful change requires a DOX pass before the task is done.

Update the closest owning AGENTS.md when a change affects:

- purpose, scope, ownership, or responsibilities
- durable structure, contracts, workflows, or operating rules
- required inputs, outputs, permissions, constraints, side effects, or artifacts
- user preferences about behavior, communication, process, organization, or quality
- AGENTS.md creation, deletion, move, rename, or index contents

Update parent docs when parent-level structure, ownership, workflow, or child index changes. Update child docs when parent changes alter local rules. Remove stale or contradictory text immediately. Small edits that do not change behavior or contracts may leave docs unchanged, but the DOX pass still must happen.

## Hierarchy

- Root AGENTS.md is the DOX rail: project-wide instructions, global preferences, durable workflow rules, and the top-level Child DOX Index
- Child AGENTS.md files own domain-specific instructions and their own Child DOX Index
- Each parent explains what its direct children cover and what stays owned by the parent
- The closer a doc is to the work, the more specific and practical it must be

## Child Doc Shape

- Create a child AGENTS.md when a folder becomes a durable boundary with its own purpose, rules, responsibilities, workflow, materials, or quality standards
- Work Guidance must reflect the current standards of the project or user instructions; if there are no specific standards or instructions yet, leave it empty
- Verification must reflect an existing check; if no verification framework exists yet, leave it empty and update it when one exists

Default section order:
- Purpose
- Ownership
- Local Contracts
- Work Guidance
- Verification
- Child DOX Index

## Style

- Keep docs concise, current, and operational
- Document stable contracts, not diary entries
- Put broad rules in parent docs and concrete details in child docs
- Prefer direct bullets with explicit names
- Do not duplicate rules across many files unless each scope needs a local version
- Delete stale notes instead of explaining history
- Trim obvious statements, repeated rules, misplaced detail, and warnings for risks that no longer exist

## Closeout

1. Re-check changed paths against the DOX chain
2. Update nearest owning docs and any affected parents or children
3. Refresh every affected Child DOX Index
4. Remove stale or contradictory text
5. Run existing verification when relevant
6. Report any docs intentionally left unchanged and why

## User Preferences

When the user requests a durable behavior change, record it here or in the relevant child AGENTS.md

## Child DOX Index

This project is not yet indexed. Before continuing you must scan the project, build the DOX tree and replace this message with the actual index. Go deep and scan files recursively to properly evaluate complexity and create nested DOX files where needed.