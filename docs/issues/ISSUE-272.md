# ISSUE-272: Web — Editable Entity Tags and Material/Entity Section Grouping

## Context & User Story
- **Goal:** As a server admin, I want to manage entity tags in the web GUI exactly like I manage material tags, and I want the two kinds visually grouped under "Material Tags" and "Entity Tags" sections so it is clear which kind each row is.
- **Agent Role:** You are an expert full-stack engineer executing this task. Entity tags are stored under `entity_tags:` in `tags/base.yml`, resolved by the engine's `EntityTagResolver` for the `target_type` state filter. Today the GUI can only view their keys read-only; this ticket makes them fully editable (add, delete, modify) with the same UX as material tags.

## Implementation Requirements
- [ ] **Backend (`TagHandler.update`):** accept an optional `entityTags` field in the `PUT /api/tags` body alongside `tags`. Validate each entity tag exactly as the engine does (accept `#minecraft:` vanilla entity tags, `#c:` cross-references, or bare entity type names; reject unknown values with a clear 400). Write the validated map under `entity_tags:` in the staged YAML. Preserve the existing behavior that `tags` is required and that a body with neither section is invalid.
- [ ] **Backend (`GET /api/tags`):** unchanged — `entityTags` already returns the full map (not just keys) from `TagHandler.get`.
- [ ] **Frontend API (`client.ts`):** extend `api.tags.update` to send `{ tags, entityTags }` (both sections), and update the response/request types so entity tags are a full `Record<string, string[]>`.
- [ ] **Frontend (`TagsPage.vue`):** replace the read-only entity-tag key chips (template lines 36-42, script `entityTagKeys` line 86) with a full editable model `Record<string, string[]>` that participates in add/delete/modify, search filtering, dirty tracking, and the save payload exactly like material tags.
- [ ] **Visual grouping:** render the page in two distinct sections, "Material Tags" and "Entity Tags", each with its own `TagListEditor` instance and a clear section heading. Both sections share the `#c:` key prefix; `TagListEditor` already hardcodes that prefix when adding (`TagListEditor.vue` line 64), which is correct for both kinds.
- [ ] **Validation parity:** entity tag values must be validated client-side or via the backend so a saved file is never loadable-rejected by `CustomTagLoader.validateEntityEntry`. Reuse the backend's `validateEntityEntry` semantics; the existing material validation path is the model.
- [ ] Keep the dirty/save/cancel `StickyActionBanner` flow working for both sections; a change in either section must mark the page dirty and a save must stage both sections in one `PUT`.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/TagHandler.java` (`update` lines 57-107 currently preserves `entity_tags` read-only via `loadEntityTags()` at lines 116-128; extend to write them explicitly)
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/CustomTagLoader.java` (`validateEntityEntry` semantics, lines ~185-198)
  - `web/frontend/src/api/client.ts` (`tags.update` line 60, `tags.get` line 59)
  - `web/frontend/src/views/TagsPage.vue` (read-only entity chips at lines 36-42, `entityTagKeys` line 86, `saveTags` lines 187-198, `leaveSave` lines 148-164)
  - `web/frontend/src/components/tags/TagListEditor.vue` (reusable editable list; `#c:` prefix at line 64)
  - `web/frontend/src/components/tags/MaterialMultiSelect.vue` (generic value multi-select; placeholder text is material-specific, consider entity wording)
  - E2E: `web/frontend/e2e/pages/TagsPage.ts`, `web/frontend/e2e/specs/tags.spec.ts` (`.tag-header` selectors will match rows in both sections once entity tags are editable — scope them per section)
- **Dependencies:** none.
- **Constraints:** Per `web/AGENTS.md`, use the Composition API and keep the build free of dead code. A GUI save must never silently delete a section the user did not edit (the current code preserves `entity_tags`; after this ticket both are explicit). Material tag behavior must remain unchanged.

## Verification & Definition of Done
- [ ] `./gradlew build`, `./gradlew test`, and `cd web/frontend && npm run build` pass.
- [ ] Entity tags can be added, renamed, reordered, edited, and deleted in the GUI, and a staged save writes them to `entity_tags:` such that the reloaded `/api/tags` and the engine's `EntityTagResolver` reflect them.
- [ ] The page shows "Material Tags" and "Entity Tags" sections; a change in either sets the dirty state and both save together.
- [ ] Invalid entity values (unknown entity type / unknown vanilla tag) are rejected with a clear error and the file is not staged.
- [ ] Existing material-tag E2E assertions still pass; new E2E coverage asserts entity tags round-trip through the PUT body.

## Issues
- Related: the two sections reuse `TagListEditor`, which removes the last row guard and add-form validation; confirm per-section remove-when-single and id-pattern behavior is preserved for entity tags too.
