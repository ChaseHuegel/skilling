# ISSUE-251: Tags — Relocate to `tags/base.yml` and Load Recursively with Additive Merge

## Context & User Story
- **Goal:** As a server owner, I want to keep my custom tag definitions in a `tags` data folder where each `.yml` file loads recursively. I want the shipped default renamed from `tags.yml` to `tags/base.yml`. I want a tag that appears in more than one file to merge additively in memory. I want a file that cannot be read as tags to log a warning and be skipped, never failing the load.
- **Agent Role:** You are an expert backend engineer executing this task. Additive merge means the entry lists of a duplicated tag key are appended together, so the contents of one file never overwrite the contents of another file in the in-memory tag map.

## Implementation Requirements
- [ ] Rename `src/main/resources/tags.yml` to `src/main/resources/tags/base.yml` (content unchanged).
- [ ] Add `CustomTagLoader.loadDirectory(File tagsDir)`: walk the directory recursively; collect `.yml` files sorted by relative path for a deterministic order; gather the raw `custom_tags` and `entity_tags` entries from every file; append entry lists when a tag key repeats across files (additive merge); resolve all tags in one global pass so cross-file `#c:` references work; treat a missing directory as an empty store and mark the loader `loaded`.
- [ ] A `.yml` file in the `tags` directory that cannot be read as tags (non-map YAML, scalar where a list is expected, unknown material or entity name, unknown vanilla tag) logs a warning and is skipped. It does NOT fail the load.
- [ ] Keep `CustomTagLoader.load(File)` for single-file loading; share the gather/resolve machinery between both entry points.
- [ ] First-run provisioning in `Skilling.onEnable` generates `tags/base.yml` instead of `tags.yml`; the `tags` data directory is created when missing.
- [ ] Startup (`Skilling.onEnable`) and reload (`LockdownManager.rebuild`) load the whole `tags` directory via `loadDirectory`.
- [ ] Web stays working: `WebServer`, `StagingManager`, and `TagHandler` read and write `tags/base.yml` only; staging status path strings use `tags/base.yml`.
- [ ] E2E fixture `web/frontend/e2e/test-data/tags.yml` moves to `web/frontend/e2e/test-data/tags/base.yml`; `globalSetup` copies it to `plugins/Skilling/tags/base.yml`.

## Technical Specifications & Context
- **Target Files:**
  - Renamed: `src/main/resources/tags.yml` -> `src/main/resources/tags/base.yml`
  - Modified: `src/main/java/io/github/chasehuegel/skilling/engine/tag/CustomTagLoader.java`, `src/main/java/io/github/chasehuegel/skilling/Skilling.java`, `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java`, `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java`, `src/main/java/io/github/chasehuegel/skilling/web/staging/StagingManager.java`, `src/main/java/io/github/chasehuegel/skilling/web/handler/TagHandler.java`
  - Test updates (keep the suite green): `src/test/.../web/handler/HandlerErrorHandlingTest.java`, `src/test/.../web/handler/TagHandlerEntityTagsRoundTripTest.java`, `src/test/.../web/staging/StagingManagerConcurrencyTest.java`
  - E2E: `web/frontend/e2e/test-data/tags/base.yml`, `web/frontend/e2e/globalSetup.ts`
- **Dependencies:** none.
- **Constraints:** Per `src/AGENTS.md`, the entity tags store is resolved by the parallel `EntityTagResolver`. Apply the same additive merge semantics to `entity_tags` for consistency. New automated coverage for directory loading lives in ISSUE-256.

## Verification & Definition of Done
- [ ] `./gradlew build` passes.
- [ ] `./gradlew test` passes.
- [ ] `cd web/frontend && npm run build` passes.
- [ ] A fresh data folder generates `tags/base.yml`. A second file `tags/custom.yml` defining an existing tag key merges its entries into the in-memory tag map.
- [ ] A malformed `.yml` in the `tags` directory logs a warning and does not abort the load.
