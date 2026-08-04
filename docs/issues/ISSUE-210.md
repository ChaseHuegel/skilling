# ISSUE-210: Preserve the `entity_tags:` section when saving tags.yml via the web GUI

## Context & User Story
- **Goal:** As a server admin, I want saving tags from the web GUI to keep the `entity_tags:` section intact so `target_type:#c:...` filters and the `EntityTagResolver` keep working.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `TagHandler.update` must round-trip the existing `entity_tags:` section instead of rebuilding tags.yml from a map that only contains `custom_tags`.
- [x] The staged tags.yml preserves ordering and comments for the untouched `entity_tags:` section, or at minimum preserves all keys and values (including `entity_tags`).
- [x] Add a regression test (web handler test) that stages a tags.yml containing both `custom_tags` and `entity_tags` and asserts the staged file still contains the original `entity_tags` entries.
- [x] If the GUI should also edit entity tags, extend the API contract and frontend accordingly; otherwise document that entity tags are read-only in the GUI and preserved on save.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/TagHandler.java` (lines 49-91)
  - `src/test/java/io/github/chasehuegel/skilling/web/handler/*` (existing handler security/validation tests)
  - `web/frontend/**` (only if entity-tag editing is added)
- **Dependencies:** `EntityTagResolver` reads `entity_tags` via `CustomTagLoader` (see `LockdownManager.java:84-87` reload wiring).
- **Constraints:** Data loss is the bug: the shipped `tags.yml:258` defines `#c:undead` etc. and a GUI save silently deletes them, silently breaking any skill using `state: "target_type:#c:undead"` (which is also not load-validated — see ISSUE-217).

## Verification & Definition of Done
- [x] After staging and applying tags via the API, `entity_tags:` entries from the original file still resolve through `EntityTagResolver`.
- [x] Regression test passes (`./gradlew test`).
- [x] `./gradlew build` passes.
