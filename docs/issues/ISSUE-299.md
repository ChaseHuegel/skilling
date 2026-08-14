# ISSUE-299: Web GUI behavior fixes — reload rollback, staged reads, performance, and serialization

## Context & User Story
- **Goal:** As an admin, I want the web GUI to never wedge the server on a failed reload, to never destroy a pending config/tags edit, and to stay fast and correct under normal use.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] **Failed reload should roll back live files:** `ReloadHandler.java:47-97` copies staged files into the live tree before the lockdown rebuild; on failure the live files stay changed while the engine keeps old state, and the retry re-applies the same broken files. On reload failure, restore the affected files from the current backup directory (or validate against a dry-run reload before copying).
- [x] **Config/Tags GET must reflect the staged file:** `ConfigHandler.java:30-80` and `TagHandler.java:32-59` read the live files, so a second save before reload silently destroys the first pending edit. Mirror `SkillHandler`/`GuiLayoutHandler`: read the staged file when one exists and merge PUTs on top of staged content.
- [x] **`GET /api/skills/{id}` is O(N) YAML-parse per request:** `SkillHandler.java:171-207` walks and parses the whole skills tree for any nested/renamed id. Cache an `id → relative path` index, invalidated on reload/apply/stage.
- [x] **Duplicate skill ids on one GUI page silently dropped:** `GuiLayoutSerializer.java:193-196` inverts `slots` via a map, so `{0: "mining", 5: "mining"}` keeps only one. Validate that a skill id appears at most once per page.
- [x] **Reload is not serialized:** two concurrent `POST /api/reload` interleave the lockdown sequence. Guard with a single-flight flag.
- [x] **Conflicts mislabeled 500 instead of 409:** `ReloadHandler.java:50-57` maps both "nothing applied" and "conflict at locked re-check" to 500. Distinguish the conflict case (409) from a true server error.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:47-97`, `src/main/java/io/github/chasehuegel/skilling/web/handler/ConfigHandler.java:30-80,188-189,248-249`, `src/main/java/io/github/chasehuegel/skilling/web/handler/TagHandler.java:32-59,132`, `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:171-207`, `src/main/java/io/github/chasehuegel/skilling/web/dto/GuiLayoutSerializer.java:193-196`, `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java:75-91,199-201,276`.
- **Dependencies:** ISSUE-291 (staging clear window) touches `ReloadHandler` too; coordinate to avoid merge conflicts.
- **Constraints:** Preserve the staging/apply/backup invariants. The `id → path` cache must be invalidated on every apply and reload.

## Verification & Definition of Done
- [x] New tests: failed reload restores backups; staged config/tags survives a second save; skill lookup hits the cache; duplicate page skill ids rejected; concurrent reloads serialized; conflict returns 409.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: normal save/apply/reload flows unchanged.
