# ISSUE-290: Deleting or renaming a nested skill through the GUI silently does nothing

## Context & User Story
- **Goal:** As an admin, I want to delete or rename a skill that lives in a `skills/subfolder/*.yml` file from the web GUI, and I want the live file actually removed. Deletion markers record only the bare id; `applyAndBackup` resolves them to the flat path `skills/<id>.yml`, so the real nested file survives. The API returns `{"status":"ok"}` and the reload keeps the skill. Renames out of a subfolder write the new flat file but never remove the nested original, so both skills end up loaded.
- **Agent Role:** You are an expert backend/QA engineer executing this task.

## Implementation Requirements
- [ ] Persist the resolved live-file relative path in the deletion marker (or a sidecar mapping) at `stageSkillDeletion` time, and have `applyAndBackup` delete exactly that path.
- [ ] Apply the same path resolution to rename-out-of-subfolder flows so the original nested file is removed.
- [ ] Add a test: delete `skills/special/blasting.yml` through the API and assert the live file is gone after apply.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:218-221` (flat-path deletion resolution), `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:112-150` (rename/delete). Existing test `src/test/java/io/github/chasehuegel/skilling/web/handler/SkillHandlerSecurityTest.java:159-170` only asserts the marker exists, not that the live file is removed.
- **Dependencies:** None.
- **Constraints:** Keep the id regex validation and `confineTo` traversal guards. The delete marker format is internal to the staging directory; changing it is allowed (greenfield).

## Verification & Definition of Done
- [ ] New nested-skill delete/rename tests pass.
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: deleting a flat-path skill behaves exactly as before.
