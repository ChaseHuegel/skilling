# ISSUE-269: Skill and GUI-layout editors show the live file, hiding staged edits after save

## Context & User Story
- **Goal:** As an admin, I want my just-saved staged edits to remain visible in the editor after saving, so a reload doesn't appear to have failed and I never re-save against stale content.
- **Agent Role:** You are an expert frontend engineer executing this task.
- **Severity:** Medium — confusing UX that can lead admins to overwrite their own staged edits.

## Implementation Requirements
- [x] Make `SkillHandler.get` / `resolveSkillFile` prefer the staged file over the live file when one exists (consistent with the create path), so updates show staged changes.
- [x] Add a staged-file fallback to `GuiLayoutHandler.get` so the layout editor also reflects pending changes.
- [x] Add an e2e spec (or unit tests) covering save → re-fetch shows the staged content.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:141-147` (`resolveSkillFile` returns `confinedLiveFile` first, falling back to staged only when no live file exists)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/GuiLayoutHandler.java:44-57` (`get` reads the live file only, no staged fallback)
  - `web/frontend/src/views/SkillEditorPage.vue:477`, `web/frontend/src/views/GuiLayoutPage.vue:160-168` (reload after save)
  - `src/test/java/io/github/chasehuegel/skilling/web/handler/GuiLayoutHandlerValidationTest.java`
- **Dependencies:** none.
- **Constraints:** The staging banner must continue to distinguish pending vs applied; read paths must still serve the live file when no staged edit exists. Keep the existing editor behavior for new (staged-only) skills unchanged.

## Verification & Definition of Done
- [x] After saving an edit, re-fetching the skill/layout shows the staged content.
- [x] Unedited skills still show live content.
- [x] `cd web/frontend && npm run build` and `./gradlew build` / `./gradlew test` pass.
