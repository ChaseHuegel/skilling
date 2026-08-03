# ISSUE-189: Document `cure_villager` trigger and entity tags in user docs

## Context & User Story
- **Goal:** As a server owner / addon author, I want the capability catalog and configuration reference to list the `cure_villager` trigger and the `entity_tags:` tag store so I can author skills that use them.
- **Agent Role:** You are an expert technical writer executing this task.

## Implementation Requirements
- [x] Add `cure_villager` to `docs/users/capabilities.md` (trigger, event, parameters: none; scope note — fires only on CURED transforms, player must be online at conversion completion).
- [x] Document `target_type` state filter now matching `EntityDeathEvent` and accepting `#...` entity tags, in `docs/users/capabilities.md`.
- [x] Document the `entity_tags:` section of `tags.yml` (syntax, `#c:` prefix, vanilla `#minecraft:*` cross-refs, `EntityType` values) in `docs/users/configuration.md`.
- [x] Keep the docs free of implementation jargon; cross-link between the two files where relevant.

## Technical Specifications & Context
- **Target Files:** `docs/users/capabilities.md`, `docs/users/configuration.md`
- **Dependencies:** ISSUE-186 and ISSUE-187 merged (the features this documents).
- **Constraints:** Documentation standards per `docs/AGENTS.md`; YAML template comments per `src/AGENTS.md` §8 (also apply to `tags.yml` header comments).

## Verification & Definition of Done
- [x] Docs render as clean Markdown; cross-references resolve.
- [x] `./gradlew build` passes (docs-only ticket; no code changes).
- [x] The shipped `tags.yml` header comments document the new `entity_tags:` section with a commented-out example (delivered with ISSUE-186).
