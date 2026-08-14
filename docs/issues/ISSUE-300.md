# ISSUE-300: Build/CI/frontend hygiene — duplicate tests, E2E artifacts, relocation, and doc drift

## Context & User Story
- **Goal:** As a maintainer, I want CI fast and debuggable, the shaded JAR free of collision-prone duplicates, and frontend docs accurate.
- **Agent Role:** You are an expert build/CI engineer executing this task.

## Implementation Requirements
- [ ] **CI runs tests twice and double-caches Gradle:** `ci.yml:53-57` runs `./gradlew build` then `./gradlew test` (`build` already runs tests). Drop the redundant step. Remove the manual `actions/cache` for `~/.gradle/caches` in favor of `gradle/actions/setup-gradle@v4`'s caching.
- [ ] **E2E job lacks Gradle cache and artifact upload:** `.github/workflows/e2e.yml` boots a real Paper server; on a flaky boot there is nothing debuggable. Add Gradle caching and an `upload-artifact` step for Playwright traces/reports on failure.
- [ ] **Unrelocated duplicates in the shaded JAR:** `slf4j-api`/`jul-to-slf4j` (duplicate Paper's bundled 2.0.16; `jul-to-slf4j` is unused) and `kotlin-stdlib` (Javalin's) ship unrelocated. Use `compileOnly` for `slf4j-api` (Paper provides it), drop `jul-to-slf4j`, and relocate `kotlin`/`org.jetbrains` (or document why not).
- [ ] **Frontend asset version drift:** `web/frontend/.env` pins `VITE_MINECRAFT_ASSETS_VERSION=1.21.4` while the server/API target is 1.21.8 (`MinecraftIcon.vue:43` fallback is the same). Bump to a 1.21.8 asset branch if available, or update `web/AGENTS.md` to reflect the deliberate pin.
- [ ] **Hardcoded default password in the SPA fallback:** `ConfigPage.vue:244` ships `password: 'skilling'` in the fallback state. Default the field to `''` (blank = keep current).
- [ ] **AGENTS.md route table drift:** `web/AGENTS.md` omits `/api/mechanics`, `/api/triggers`, `/api/state-filters` (registered in `WebServer.java:184-196`, used by `client.ts:76-83`). Update the table.

## Technical Specifications & Context
- **Target Files:** `.github/workflows/ci.yml:24-28,53-57`, `.github/workflows/e2e.yml`, `build.gradle.kts:39-40` (slf4j deps), `build.gradle.kts:32` (Javalin/kotlin-stdlib), `web/frontend/.env:4`, `web/frontend/src/components/common/MinecraftIcon.vue:43`, `web/frontend/src/views/ConfigPage.vue:244`, `web/AGENTS.md` (route table), `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java:184-196`.
- **Dependencies:** ISSUE-279 (shadow relocation) overlaps with the relocation items here; coordinate.
- **Constraints:** Per `src/AGENTS.md`, never break the Jackson compileOnly contract with Paper's runtime.

## Verification & Definition of Done
- [ ] CI runs tests once and keeps one Gradle cache path.
- [ ] E2E job uploads failure artifacts.
- [ ] Shaded JAR has no duplicate `org/slf4j` payload and a documented kotlin placement.
- [ ] Frontend builds and the Config editor defaults the password field to blank.
- [ ] `./gradlew build` and `cd web/frontend && npm run build` pass.
