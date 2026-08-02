# ISSUE-183: Research — supporting custom items in tags & filters

## Context & User Story
- **Goal:** As an admin with custom items (created by plugins, datapacks, commands, or other means), I want to match those items in Skilling tags & filters without Skilling integrating with any specific custom-item solution.
- **Agent Role:** You are an expert backend (Paper) engineer producing a research report — no production code changes.

Custom items vary greatly in how they are created. Skilling should **not** require per-solution integrations. Instead, admins integrate with Skilling by putting specific metadata on their items that Skilling looks for, and tag entries can become expressive enough to reason about without documentation. This ticket researches and proposes the standard. Do not lock in any single direction.

## Implementation Requirements
- [x] Map the current tag/filter/requirement resolution flow and pinpoint where material-only matching limits custom items:
  - `TagResolver` (flattened `EnumSet<Material>`, O(1) event lookups)
  - `CustomTagLoader` (`tags.yml` `#c:` definitions)
  - `SkillEventListener.matchFilter` (target/state/tool)
  - `RequirementEngine` item possession/cost checks (`countItems`/`removeItems`)
- [x] Propose a metadata standard (e.g., PersistentDataContainer/NBT keys under a `skilling:` namespace) that custom items must carry to be matched, with concrete examples for common creation methods (commands, datapacks) where feasible.
- [x] Propose tag-entry syntax extensions and justify them. Explore (do not lock in): NBT/PDC-based entries, name/regex-based entries (e.g., `name:*Greatsword`), and how they compose with existing material and vanilla-tag entries.
- [x] Evaluate the performance model: the current design pre-flattens tags to `EnumSet<Material>` at load for O(1) dispatch. Item-instance matching (NBT/name) cannot be flattened the same way — propose a pre-filter/caching strategy for the hot event path and inventory scans.
- [x] Address authoring ergonomics: a person must be able to read a tag definition and reason about how it behaves without referencing documentation. Consider what `tags.yml` entries should look like, validation/fail-fast on unknown entries, and the web editor UX (`TagListEditor.vue` / `MaterialMultiSelect.vue`).
- [x] Deliver the report to `docs/reports/REPORT_CUSTOM-ITEMS.md` following the `REPORT_<TOPIC>.md` pattern, with the owning ticket and date in the header and a cross-link back to this ticket.

## Technical Specifications & Context
- **Target Files (read-only for research):**
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/TagResolver.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/CustomTagLoader.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (`matchFilter`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java` (`countItems`/`removeItems`/`resolveMaterialSet`)
  - `src/main/resources/tags.yml`
  - `web/frontend/src/components/tags/TagListEditor.vue`, `MaterialMultiSelect.vue`
  - `docs/reports/REPORT_XP-CURVE.md` and `REPORT_DUALWIELD-API.md` as report format references
- **Dependencies:** none (research only).
- **Constraints:**
  - Research ticket: no production code changes; deliverable is the report.
  - The plugin must remain independent of specific custom-item solutions; the standard should make admins integrate *with Skilling* (by adding the metadata Skilling looks for).
  - Keep the readability goal front and center for any proposed syntax.

## Verification & Definition of Done
- [x] Report exists at `docs/reports/REPORT_CUSTOM-ITEMS.md` with owning ticket + date header and a cross-link back
- [x] Report covers every Implementation Requirement above (current flow map, metadata standard proposal, syntax options with rationale, performance strategy, authoring ergonomics)
- [x] No production code changes
