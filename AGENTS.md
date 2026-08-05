# AI Agent Instructions for Skilling (DOX Root)

Skilling is a data-driven RPG skills engine for PaperMC (Minecraft). It is a rules engine, not a traditional plugin: all mechanics, triggers, and evaluators are decoupled modules, and end-users build all content in YAML.

## Golden Rule

There are ZERO hardcoded skills, levels, or abilities in the Java backend. Skilling is greenfield with no production use: breaking changes to YAML schemas, APIs, and behaviors are allowed and expected.

## Build & Verify

Package manager: Gradle (Kotlin DSL). npm is used only inside `web/frontend`.

- `./gradlew build` — compile and package the plugin.
- `./gradlew test` — run JUnit 5 unit tests.
- `cd web/frontend && npm run build` — type-check and build the web GUI (required for web-only changes).

## Commits

All commits MUST follow `docs/dev/CONVENTIONS-COMMITS.md`. No commit skips this convention.

## Read Before Editing

Read this file and every `AGENTS.md` along the path to the files you will touch. The closest doc controls local work details; no child doc weakens DOX. The subsystem map lives in the [Child DOX Index](docs/agents/DOX-FRAMEWORK.md).

## Reference

- [Project & Tech Stack](docs/agents/PROJECT.md) — purpose, ownership, package base, tech stack.
- [Issue Resolution Workflow](docs/agents/ISSUE-WORKFLOW.md) — sprint/backlog scope, per-issue sequence, validation gate.
- [DOX Framework](docs/agents/DOX-FRAMEWORK.md) — how the AGENTS.md hierarchy works.
