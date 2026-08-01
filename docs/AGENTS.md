# AI Agent Instructions for Skilling Documentation (docs)

This is the documentation subsystem of Skilling. It is the closest DOX contract for all work under `docs/`. Read the root `AGENTS.md` for project-wide rules, then use this file for documentation standards.

## Purpose

Provide the durable reference material for the project: user-facing guides for server owners and addon developers, developer-facing architecture specs and conventions, and project issue tracking.

## Ownership

- `docs/users/**` — user-facing documentation (installation, configuration, skill authoring, API integration, capability catalog).
- `docs/dev/**` — developer-facing specs and conventions (architecture, requirements, skill design framework, skill template, commit spec).
- `docs/issues/**` — issue tracking (`INDEX.md`), detailed issue write-ups, and research reports.
- `docs/AGENTS.md` itself — owns documentation content standards only; Java/Javadoc rules and config-template rules live in `src/AGENTS.md`.

## Local Contracts

### User Documentation (`docs/users/`)
The following files must exist and stay current:
* `getting-started.md` — Installation, first run, basic usage (`/skills` commands).
* `configuration.md` — Reference for `config.yml` and `tags.yml` with all supported keys.
* `creating-skills.md` — Full YAML schema for skill definitions, abilities, XP sources, and requirements, with annotated examples.
* `api-integration.md` — How to register custom mechanics, triggers, and evaluators via the API. Maven/Gradle coordinates, code samples. Keep in sync with `skilling-api/AGENTS.md`.
* `capabilities.md` — Catalog of every built-in mechanic, trigger, and evaluator with their parameters and YAML usage.

These docs are **user-facing** and must use clear language free of implementation jargon.

### Dev Documentation (`docs/dev/`)
* `REQUIREMENTS.md` — technical requirements (tech stack, schema, async pipeline).
* `DESIGN.md` — architecture & design spec (module layout, execution pipeline, UI architecture).
* `SKILL-DESIGN-FRAMEWORK.md` — content design pillars, dual-layer scaling curves, milestone template, ability audit checklist. Apply when authoring bundled skill YAML.
* `template-skill.yml` — annotated skill YAML template; every key documents supported values and defaults.
* `CONVENTIONS-COMMITS.md` — binding commit message spec for the whole repository.

### YAML Template Documentation
Every configurable YAML template (in `docs/dev/` and shipped under `src/main/resources/`) must include commented documentation for each key: supported values, defaults, and a brief description, plus commented-out examples inline.

### Issue Tracking (`docs/issues/`)
* `INDEX.md` — the issue index. Resolve items one at a time per the Issue Resolution Workflow in root `AGENTS.md`; check off bullets only when fully done.
* Detailed single-issue write-ups follow the naming pattern `ISSUE-<n>.md` and are referenced from `INDEX.md`.
* Research reports (e.g., `REPORT_XP-CURVE.md`) land here.

#### Issue Ticket Template
Every issue ticket (`ISSUE-<n>.md`) must follow this section structure (keep low-complexity items terse):

```
# ISSUE-<n>: Clear, Actionable Goal

## Context & User Story
- **Goal:** As a [role], I want to [action] so that [benefit].
- **Agent Role:** You are an expert [frontend/backend/QA] engineer executing this task.

## Implementation Requirements
- [ ] Explicit, atomic requirement statements

## Technical Specifications & Context
- **Target Files:** `path/to/file`
- **Dependencies:** ...
- **Constraints:** ...

## Verification & Definition of Done
- [ ] Automated tests / build / type-check pass
- [ ] Edge case handled: ...
```

## Work Guidance

* When a change alters a user-visible behavior or the API, update the affected `docs/users/` file in the same change.
* When a change alters engine architecture, update `docs/dev/DESIGN.md` and/or `docs/dev/REQUIREMENTS.md`.
* Keep `template-skill.yml` in sync with the skill YAML schema accepted by `SkillManager`.

## Verification

No automated checks. Content is validated by the issue workflow (mark-complete gate) and human review on commit. Ensure docs render as clean Markdown and cross-references stay valid.

## Child DOX Index

None.
