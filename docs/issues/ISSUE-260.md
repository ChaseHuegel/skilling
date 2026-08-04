# ISSUE-260: Renaming a skill to an existing ID silently overwrites the live skill on Apply

## Context & User Story
- **Goal:** As an admin, I want renaming a skill in the web GUI to an ID that already exists to be rejected up front, never to silently destroy the existing skill's content on Apply.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — data loss in the admin tool. `PUT /api/skills/{oldId}` with a body whose `id` differs stages the new file and a deletion marker for the old one, but nothing checks that `newId` collides with a live skill. The frontend duplicate check only runs for new skills (`isNew && ...`).

## Implementation Requirements
- [x] Reject `PUT /api/skills/{oldId}` with a 400 when `newId != oldId` and a live `skills/{newId}.yml` (or an existing skill file whose parsed ID equals `newId`) exists.
- [x] Mirror the duplicate-ID check in `SkillEditorPage.vue` for edit-mode renames, so the editor flags it before the request.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:101-124` (update), `:141-162` (`resolveSkillFile` — the collision test should reuse the filename-and-parsed-ID matching)
  - `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:221-233` (apply copies staged over live with `REPLACE_EXISTING`; only the on-disk backup survives, no restore UI)
  - `web/frontend/src/views/SkillEditorPage.vue:145` (duplicate check gated on `isNew`)
  - `src/test/java/io/github/chasehuegel/skilling/web/handler/SkillHandlerStagingValidationTest.java`
- **Dependencies:** none.
- **Constraints:** Only block genuine collisions; renaming to a *deleted* or never-existing ID must keep working.

## Verification & Definition of Done
- [x] Rename-onto-existing-ID returns 400 and stages nothing.
- [x] Legitimate renames still work.
- [x] `./gradlew build`, `./gradlew test`, and `cd web/frontend && npm run build` pass.
