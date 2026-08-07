# ISSUE-257: Docs and DOX Pass for Portability Loading

## Context & User Story
- **Goal:** As a reader, I want the documentation and agent instructions to describe the new data layout: `tags/base.yml` with recursive additive tag files, the optional `abilities` folder with id-referenced polymorphic abilities, and recursive skill folders.
- **Agent Role:** You are an expert technical writer executing this task. Apply the ASD-STE100 writing standard from `docs/AGENTS.md` to all prose.

## Implementation Requirements
- [x] `docs/users/getting-started.md`: update the first-run file tree to show `tags/base.yml`, the `abilities/` folder, and recursive `skills/` folders.
- [x] `docs/users/configuration.md`: replace the `## tags.yml` section with a `tags/` section documenting `base.yml`, the additive merge of a duplicated tag key, recursive loading, and the warn-and-skip behavior for unreadable files.
- [x] `docs/users/creating-skills.md`: document the `abilities` folder and the polymorphic ability schema — reference by id, and per-skill field overrides on top of the base definition.
- [x] `docs/users/capabilities.md` and `docs/users/api-integration.md`: update the `tags.yml` references to `tags/base.yml`.
- [x] `docs/dev/DESIGN.md` and `docs/dev/REQUIREMENTS.md`: update the tags section, add the `AbilityManager` to the module layout, and add the ability-registry requirement.
- [x] `src/main/resources/template-skill.yml` and `docs/dev/template-skill.yml`: document the ability `id` reference and override semantics in the abilities section.
- [x] `src/AGENTS.md`: update the `tags.yml` references and add a caveat to section 9 that organizational loading (tags, skills, abilities) warns and skips instead of failing fast.
- [x] `web/AGENTS.md`: update the route-table and test-data references to `tags/base.yml` and record the no-editor-support follow-up for referenced abilities and nested-skill editing.
- [x] `README.md`: update the `tags.yml` reference.
- [x] Leave `docs/reports/REPORT_CUSTOM-ITEMS.md` unchanged; it is a historical research deliverable.

## Technical Specifications & Context
- **Target Files:** the files listed above.
- **Dependencies:** ISSUE-251 through ISSUE-256 must land first so the docs match the shipped behavior.
- **Constraints:** Per `docs/AGENTS.md`, update the affected `docs/users/` file in the same change as the behavior change where practical. Use one name for one thing and write instructions of at most 20 words.

## Verification & Definition of Done
- [x] All prose follows the ASD-STE100 standard.
- [x] Cross-references in the docs stay valid; the docs render as clean Markdown.
- [x] The docs describe behavior that matches the implementation from ISSUE-251 through ISSUE-256.
