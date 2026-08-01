# AI Agent Instructions for Skilling (DOX Root)

Welcome to the Skilling repository. This file is the **root DOX rail**: it carries project-wide instructions, global preferences, durable workflow rules, and the top-level Child DOX Index. Read it before writing or modifying any code, then read every AGENTS.md along the path to the files you will touch.

Companion deeper specs: `README.md`, `docs/dev/REQUIREMENTS.md`, `docs/dev/DESIGN.md`, `docs/dev/SKILL-DESIGN-FRAMEWORK.md`, `docs/dev/template-skill.yml`, and `docs/dev/CONVENTIONS-COMMITS.md` (commit message format).

## Purpose

Skilling is a high-performance, data-driven RPG skills engine for PaperMC (Minecraft). It acts as a rules engine, not a traditional plugin.
* **The Golden Rule:** There are ZERO hardcoded skills, levels, or abilities in the Java backend.
* All mechanics, triggers, and evaluators are decoupled modules.
* Content is constructed entirely via YAML configurations by the end-user.
* The system is designed to maintain 20 TPS under heavy load.

## Ownership

This file owns the entire repository. Durable sub-boundaries (engine, API, web GUI, docs) are delegated to the child AGENTS.md files indexed below; anything not claimed by a child remains owned here.

## Local Contracts

### Package Base
All code lives under `io.github.chasehuegel.skilling`.

### Tech Stack
* **Target API:** Paper API (Latest release)
* **Language:** Java 21 (LTS) — *Use modern features: Records, Switch Expressions, Pattern Matching.*
* **Build System:** Gradle (Kotlin DSL); `skilling-api` is a published subproject.
* **Database:** Embedded SQLite (WAL mode) with HikariCP pooling.
* **Command Framework:** Incendo Cloud (with `cloud-paper` and `cloud-annotations`).
* **Web GUI:** Javalin 7 + Vue 3 (see `web/AGENTS.md`).

### Commit Conventions
All commits MUST follow `docs/dev/CONVENTIONS-COMMITS.md`. No commit skips this convention.

### Issue Resolution Workflow
When resolving items from `docs/project/ISSUES.md`, follow this strict sequence:

1. **One issue at a time** — Tackle one issue (including all its sub-bullets) completely before starting the next.
2. **Plan first (if complex)** — For issues with sub-bullets or non-trivial scope, write a brief development plan before writing any code.
3. **Build** — After making changes, run `./gradlew build`. Fix any compiler errors before continuing.
4. **Test** — Run `./gradlew test`. Fix any test failures introduced by the changes.
5. **Self-review** — Read the diff (`git diff`) to verify correctness, style, and adherence to conventions.
6. **Mark complete** — Check off the resolved bullet(s) in `docs/project/ISSUES.md`.
7. **Commit** — `git add -A && git commit -m "..."` following `docs/dev/CONVENTIONS-COMMITS.md`.
8. **Next issue** — Repeat from step 1 for the next unchecked item.

### Validation Gate
After each development phase, `./gradlew build` AND `./gradlew test` must pass before proceeding.

## Work Guidance

* Engine implementation rules (ECS composition, thread safety, requirements engine, UI security, configs & tags, commands, Javadoc, coding style) live in `src/AGENTS.md`.
* The published addon-facing API module is governed by `skilling-api/AGENTS.md`.
* The Web GUI subsystem (frontend, REST API, E2E, asset textures) is governed by `web/AGENTS.md`.
* Documentation structure and content standards live in `docs/AGENTS.md`.

## Child DOX Index

| Path | Scope |
|---|---|
| `src/AGENTS.md` | Java plugin backend: `src/main/java/io/github/chasehuegel/skilling/engine/**`, `api/**`, `resources/**`, and `src/test/**`. Does NOT own `io.github.chasehuegel.skilling.web` (see `web/AGENTS.md`). |
| `skilling-api/AGENTS.md` | The published addon-facing API module (`skilling-api/src/**`). |
| `web/AGENTS.md` | Web GUI subsystem: `web/frontend/**`, `web/frontend/e2e/**`, and the Java backend package `io.github.chasehuegel.skilling.web`. |
| `docs/AGENTS.md` | Documentation standards and structure: `docs/users/**`, `docs/dev/**`, `docs/project/**`. |

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

| Path | Scope |
|---|---|
| `src/AGENTS.md` | Java plugin backend: `src/main/java/io/github/chasehuegel/skilling/engine/**`, `api/**`, `resources/**`, and `src/test/**`. Does NOT own `io.github.chasehuegel.skilling.web` (see `web/AGENTS.md`). |
| `skilling-api/AGENTS.md` | The published addon-facing API module (`skilling-api/src/**`). |
| `web/AGENTS.md` | Web GUI subsystem: `web/frontend/**`, `web/frontend/e2e/**`, and the Java backend package `io.github.chasehuegel.skilling.web`. |
| `docs/AGENTS.md` | Documentation standards and structure: `docs/users/**`, `docs/dev/**`, `docs/project/**`. |