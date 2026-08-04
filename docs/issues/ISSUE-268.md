# ISSUE-268: ConfigHandler accepts untyped and out-of-range config values

## Context & User Story
- **Goal:** As a server admin, I want a malformed web request (wrong types, negative debounce intervals, zero pool sizes) rejected with a 400, never silently written into `config.yml` where Bukkit's typed getters fall back to defaults or the engine receives values it never expected.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — the web API is a trust boundary that currently relies entirely on the SPA's validation.

## Implementation Requirements
- [ ] Validate each `ConfigHandler.update` field's type and range server-side (mirroring the frontend constraints) and return 400 on violation, before any `liveConfig.set(...)`.
- [ ] Add unit tests for wrong-typed and out-of-range payloads.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ConfigHandler.java:91-113` (raw `liveConfig.set(...)` with no checks)
  - `src/test/java/io/github/chasehuegel/skilling/web/handler/ConfigHandlerSecurityTest.java`
- **Dependencies:** none.
- **Constraints:** Do not reject `web.password` empty-string ("keep current") or `web.allowed_origins` list; keep the merge-preserves-unmodeled-keys behavior. Note `ConfigHandler` is web-owned but `config.yml` defaults are documented in `src/main/resources/config.yml`.

## Verification & Definition of Done
- [ ] Wrong-typed or out-of-range values return 400 and leave `config.yml` untouched.
- [ ] Valid payloads still apply.
- [ ] `./gradlew build` and `./gradlew test` pass.
