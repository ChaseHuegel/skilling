# ISSUE-301: Design decision — hardcoded gameplay tables vs the data-driven golden rule

## Context & User Story
- **Goal:** As a maintainer, I want a conscious, documented decision about gameplay tables that currently live in Java, because the project's golden rule is that end-users build all content in YAML.
- **Agent Role:** You are an expert backend engineer and content-design analyst executing this task. This ticket involves research and a decision; production code changes are optional depending on the outcome.

## Implementation Requirements
- [x] Catalog every hardcoded gameplay-data table in the backend, including at minimum:
  - `AutoSmeltMechanic.java:43-52` (`SMELT_MAP` raw→smelted pairs)
  - `OffhandStrikeMechanic.java:60-75` (`BASE_DAMAGE` per material)
  - `AutoReplantMechanic.java:50-55` (replantable crop list)
  - `PotionEffectResolver.java:21-39` and `ModifyAttributeMechanic.java:33-44` (legacy numeric-ID tables)
  - `SkillEventListener.java:970-990` (`projectileToMaterial`)
- [x] For each table, decide and record one of:
  1. Expose it as data-driven YAML (tags, `mechanic_maps`/ability-mapping config, or similar), or
  2. Keep it as an immutable, documented library mechanic with a rationale.
- [x] Update `docs/dev/REQUIREMENTS.md`, `docs/users/capabilities.md`, and `docs/dev/SKILL-DESIGN-FRAMEWORK.md` to match the decision.
- [x] If the decision is to expose any table in YAML, implement it with schema validation and fail-fast parsing; otherwise make the immutability explicit in the mechanic Javadoc.

## Decision (outcome of this ticket)

Keep every catalogued table immutable in Java; expose none in YAML.

Each table is one of four kinds, none of which is author-facing content:
vanilla mirrors (`SMELT_MAP`, `BASE_DAMAGE`), a capability boundary (replant
crop list), deprecated compatibility shims (legacy numeric-ID tables), or
engine plumbing (`projectileToMaterial`). Exposing them would add YAML surface
that must stay in sync with each Minecraft version while providing no authoring
value. The data boundary: author-tunable tuning (rewards, chances, durations,
limits) always belongs in YAML as evaluator parameters or filters, never in a
mechanic table.

## Technical Specifications & Context
- **Target Files:** The mechanic sources listed above; docs at `docs/dev/*` and `docs/users/capabilities.md`. See also `docs/reports/` for prior research-report conventions.
- **Dependencies:** None.
- **Constraints:** These tables are not skills, levels, or abilities, so they do not literally violate the golden rule; the question is where the boundary sits. The decision must not silently change existing bundled-skill behavior.

## Verification & Definition of Done
- [x] The decision is documented in the affected docs and this ticket.
- [x] If code changes, `./gradlew test` and `./gradlew build` pass.
- [x] If docs only, the writing standard in `docs/AGENTS.md` is followed.
- [x] Edge case handled: bundled skills keep their current in-game behavior under either option.
