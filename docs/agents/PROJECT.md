# Skilling Project & Tech Stack

## Purpose

Skilling is a data-driven RPG skills engine for PaperMC (Minecraft). It acts as a rules engine, not a traditional plugin.

- **The Golden Rule:** There are ZERO hardcoded skills, levels, or abilities in the Java backend.
- All mechanics, triggers, and evaluators are decoupled modules.
- The end-user constructs all content via YAML configurations.
- The system maintains 20 TPS under heavy load.
- **Greenfield / no backwards-compatibility concern:** Skilling has no production use. Breaking changes to YAML schemas, APIs, and behaviors are allowed and expected. Do not preserve or design around backwards compatibility, and do not accept compat costs in proposals or reviews.

## Ownership

The repository is owned at the root. Durable sub-boundaries (engine, API, web GUI, docs) are delegated to the child AGENTS.md files indexed in the DOX Framework's Child DOX Index. Anything not claimed by a child remains owned at the root.

## Local Contracts

### Package Base

All code lives under `io.github.chasehuegel.skilling`.

### Tech Stack

- **Target API:** Paper API (Latest release)
- **Language:** Java 21 (LTS). Use modern features: Records, Switch Expressions, Pattern Matching.
- **Build System:** Gradle (Kotlin DSL). `skilling-api` is a published subproject.
- **Database:** Embedded SQLite (WAL mode) with HikariCP pooling.
- **Command Framework:** Incendo Cloud (with `cloud-paper` and `cloud-annotations`).
- **Web GUI:** Javalin 7 + Vue 3 (see `web/AGENTS.md`).

## Companion Specs

Deeper specs: `README.md`, `docs/dev/REQUIREMENTS.md`, `docs/dev/DESIGN.md`, `docs/dev/SKILL-DESIGN-FRAMEWORK.md`, `docs/dev/template-skill.yml`, and `docs/dev/CONVENTIONS-COMMITS.md`.
