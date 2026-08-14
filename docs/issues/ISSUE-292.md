# ISSUE-292: Frontend is silently omitted from the JAR on clean builds and never rebuilt on source changes

## Context & User Story
- **Goal:** As a developer, I want `./gradlew build` to always bundle the web frontend, and I want frontend source changes to trigger a rebuild. `processResources` evaluates `frontendDist.exists()` at configuration time (before `buildFrontend` runs), so on a clean checkout the frontend is never included in the JAR. `buildFrontend` declares `outputs.dir` but no `inputs`, so Gradle marks it UP-TO-DATE whenever `dist` exists and Vue changes are silently not rebuilt. `onlyIf` also skips the frontend silently when `node_modules` is absent.
- **Agent Role:** You are an expert build engineer executing this task.

## Implementation Requirements
- [x] Wire the frontend source into the JAR unconditionally, e.g. `from(buildFrontend)` or `from(buildFrontend.map { ... })`, so the decision is made at execution time.
- [x] Declare `inputs.dir("src")` (plus `package.json`, `vite.config.ts`) on `buildFrontend` so source changes invalidate it.
- [x] Fail or loudly warn when `node_modules` is missing instead of silently degrading.
- [x] Add a verification step: after `./gradlew clean build`, assert the JAR contains the built frontend assets under `web/frontend`.

## Technical Specifications & Context
- **Target Files:** `build.gradle.kts:60-80` (`shouldBuildFrontend`, `buildFrontend` task, `processResources` wiring) and `:82-84` (`shadowJar` dependsOn).
- **Dependencies:** CI currently works only because `ci.yml` runs `npm run build` manually before Gradle.
- **Constraints:** Do not force a frontend build when running `runServer` in a dev loop without Node. Use Gradle task wiring, not shell commands.

## Verification & Definition of Done
- [x] `./gradlew clean build` on a fresh checkout produces a JAR containing the frontend assets.
- [x] Editing a `.vue` file and rerunning `buildFrontend` rebuilds (not UP-TO-DATE).
- [x] `cd web/frontend && npm run build` still passes.
