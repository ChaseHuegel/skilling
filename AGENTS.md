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

Resolving issues is **fully autonomous and implicit**: asking to "work the active sprint / milestone" or "resolve the backlog" invokes this workflow — no per-issue prompt is required. The subsystem rules for every path a ticket touches (via `src/`, `skilling-api/`, `web/`, `docs/` AGENTS files) apply on top of these steps.

**Scope gating:**
- **Active Sprint / Current Milestone** items in `docs/issues/INDEX.md` are the default target whenever issue work is requested.
- **Backlog** items are worked ONLY when explicitly asked to work on the backlog.

**Per-issue sequence — one issue at a time, no parallelization:**

1. **Load the ticket** — Open the `ISSUE-<n>.md` write-up referenced in `INDEX.md`. It is the authoritative spec (Context & User Story, Implementation Requirements, Technical Specifications, Verification & Definition of Done). Read the DOX chain for every path you expect to touch.
2. **Plan** — For any non-trivial ticket, write a brief plan before coding. Research freely (existing implementations, Paper/Vue APIs, subsystem AGENTS docs, `docs/dev/DESIGN.md` / `docs/dev/REQUIREMENTS.md`). Do not guess APIs.
3. **Build** — Make the change, then run `./gradlew build` (and `cd web/frontend && npm run build` for web-only changes). Fix all errors before continuing.
4. **Test** — Run `./gradlew test` (plus relevant web checks). Fix all failures. Do not block on flaky E2E infrastructure.
5. **Self-review loop** — Read `git diff`. Verify correctness, style, and subsystem conventions (thread safety, fail-fast parsing, ECS composition, inventory security, Vue Composition API). Cross-check EVERY checkbox in the ticket's Implementation Requirements and Verification & Definition of Done. Resolve anything you find — fix, rebuild, retest — until the ticket is fully satisfied. If ambiguous, make your best effort from codebase patterns; do not ask questions. Run the DOX "Update After Editing" pass for durable changes.
6. **Escalation** — If a hard blocker cannot be resolved or a required change is deemed out of scope, do NOT stall or ask. Create a follow-up backlog ticket (`docs/issues/ISSUE-<n>.md`, next free number, following the template in `docs/AGENTS.md`), add it to the Backlog section of `INDEX.md`, then complete and commit the current issue with a note cross-referencing the follow-up.
7. **Mark complete** — Check off the satisfied checkboxes in the ticket AND flip the `INDEX.md` bullet to `[x]`. Mark complete only when the Definition of Done is genuinely met.
8. **Commit** — `git add -A && git commit -m "<type>: ..."` following `docs/dev/CONVENTIONS-COMMITS.md`. One commit per issue.
9. **Next issue** — Repeat from step 1 for the next unchecked item in the requested scope.

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
| `docs/AGENTS.md` | Documentation standards and structure: `docs/users/**`, `docs/dev/**`, `docs/reports/**`, `docs/issues/**`. |

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
| `docs/AGENTS.md` | Documentation standards and structure: `docs/users/**`, `docs/dev/**`, `docs/reports/**`, `docs/issues/**`. |