# AI Agent Instructions for Skilling Documentation (docs)

This is the documentation subsystem of Skilling. It is the closest DOX contract for all work under `docs/`. Read the root `AGENTS.md` for project-wide rules, then use this file for documentation standards.

## Purpose

Provide the durable reference material for the project. This includes user-facing guides for server owners and addon developers. It also includes developer-facing architecture specs and conventions, research reports, and project issue tracking.

## Ownership

- `docs/users/**`: user-facing documentation (installation, configuration, skill authoring, API integration, capability catalog).
- `docs/dev/**`: developer-facing specs and conventions (architecture, requirements, skill design framework, skill template, commit spec).
- `docs/reports/**`: research reports delivered by research tickets (e.g. `REPORT_XP-CURVE.md`). All research reports live here, not in `docs/issues/`.
- `docs/issues/**`: issue tracking (`INDEX.md`) and detailed issue write-ups.
- `docs/AGENTS.md` itself: owns documentation content standards only. Java/Javadoc rules and config-template rules live in `src/AGENTS.md`.

## Local Contracts

### User Documentation (`docs/users/`)
The following files must exist and stay current:
* `getting-started.md`: Installation, first run, basic usage (`/skills` commands).
* `configuration.md`: Reference for `config.yml` and `tags.yml` with all supported keys.
* `creating-skills.md`: Full YAML schema for skill definitions, abilities, XP sources, and requirements, with annotated examples.
* `api-integration.md`: How to register custom mechanics, triggers, and evaluators via the API. Maven/Gradle coordinates, code samples. Keep in sync with `skilling-api/AGENTS.md`.
* `capabilities.md`: Catalog of every built-in mechanic, trigger, and evaluator with their parameters and YAML usage.

These docs are **user-facing** and must use clear language free of implementation jargon.

### Dev Documentation (`docs/dev/`)
* `REQUIREMENTS.md`: technical requirements (tech stack, schema, async pipeline).
* `DESIGN.md`: architecture & design spec (module layout, execution pipeline, UI architecture).
* `SKILL-DESIGN-FRAMEWORK.md`: content design pillars, dual-layer scaling curves, milestone template, ability audit checklist. Apply when authoring bundled skill YAML.
* `template-skill.yml`: annotated skill YAML template. Every key documents supported values and defaults.
* `CONVENTIONS-COMMITS.md`: binding commit message spec for the whole repository.

### YAML Template Documentation
Every configurable YAML template (in `docs/dev/` and shipped under `src/main/resources/`) must include commented documentation for each key. Document the supported values, the defaults, and a brief description. Include commented-out examples inline.

### Research Reports (`docs/reports/`)

* Research tickets (e.g., `ISSUE-107`) deliver reports to `docs/reports/` (e.g., `docs/reports/REPORT_XP-CURVE.md`) and involve no production code changes. Reports follow a `REPORT_<TOPIC>.md` naming pattern, state the owning ticket and date in their header, and cross-link back to the ticket (`../issues/ISSUE-<n>.md`).

### Issue Tracking (`docs/issues/`)
* `INDEX.md`: the issue index, split into **Active Sprint / Current Milestone** (the default work target) and **Backlog** (worked only on explicit request). Resolve items one at a time per the Issue Resolution Workflow in `docs/agents/ISSUE-WORKFLOW.md`. Flip a bullet to `[x]` only when the ticket is fully done.
* Detailed single-issue write-ups follow the naming pattern `ISSUE-<n>.md` and are referenced from `INDEX.md`. Numbers are sequential and unique. A new ticket uses the next free number and is filed under Backlog unless told otherwise.
* **Mark-complete gate:** an issue is done when its ticket's Implementation Requirements and Verification & Definition of Done checkboxes are all satisfied AND the `INDEX.md` bullet is flipped.
* Research tickets reference their deliverable report in `docs/reports/` (see above).

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

### Writing Standard

Write all documentation prose in ASD-STE100 Simplified Technical English.

* Use one name for one thing. Do not call the same item by two different names.
* Use the short common word. Use `use`, not `utilize`. Use `start`, not `commence`. Use `make sure`, not `ensure`.
* Use the active voice. Use a verb for an action.
* Write one instruction per sentence. Write a maximum of 20 words per instruction and 25 words per descriptive sentence.
* Do not use contractions.
* Do not use semicolons or em/en dashes. Split the sentence instead.
* Do not use marketing adjectives such as `seamless`, `robust`, `powerful`, or `next-generation`.
* Use American spelling.
* Apply the same rules to YAML comment blocks shipped in `docs/dev/` and `src/main/resources/`.

These rules do not apply to code, identifiers, command syntax, YAML keys, data tables, or player-facing message copy.

## Verification

No automated checks. Content is validated by the issue workflow (mark-complete gate) and human review on commit. Make sure docs render as clean Markdown, cross-references stay valid, and prose follows the writing standard above.

## Child DOX Index

None.
