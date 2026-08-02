# ISSUE-163: Implement the E2E "automatic mode" (server bootstrap + fixture seeding)

**Status:** Closed
**Type:** Bug
**Severity:** High (documented automatic mode does not exist; `npm run e2e` fails without a manual server)

---

## Context & User Story

- **Goal:** As a developer, I want `npm run e2e` to start the Paper dev server, seed fixture data, run the tests, and stop the server — exactly as `web/AGENTS.md` documents.
- **Agent Role:** You are an expert QA/frontend engineer executing this task.

## Implementation Requirements

- [x] Make `globalSetup.ts` copy `e2e/test-data/` fixtures into `run/plugins/Skilling/` and start the Paper dev server (`./gradlew runServer`) before tests run
- [x] Record the server PID so `globalTeardown.ts` actually stops it (currently it reads `../.server.pid` that nothing writes)
- [x] Preserve the `SKILLING_SERVER_URL` external-server mode as an alternative
- [x] Fix `auth.setup.ts` to honor `SKILLING_SERVER_URL` (currently hardcodes `http://localhost:8082`)
- [x] Verify `npm run e2e` works end-to-end without a manually running server

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/e2e/globalSetup.ts` (whole file)
  - `web/frontend/e2e/globalTeardown.ts:8`
  - `web/frontend/e2e/specs/auth.setup.ts:28`
  - `web/frontend/e2e/playwright.config.ts`
- **Dependencies:** `./gradlew runServer` (build.gradle.kts `runServer` task) and the fixture files in `e2e/test-data/`.
- **Constraints:** Documented behavior in `web/AGENTS.md:183-192` is the contract. External-server mode must keep working.

### Root Cause

`globalSetup.ts` only polls `/api/health` (up to 120s) and logs in. Nothing writes `../.server.pid`, so teardown is a no-op, and fixtures are never seeded. `npm run e2e` without a manually running server fails after the health-check timeout.

### Proposed Fix

Add fixture seeding and server startup (with PID file) to `globalSetup.ts`, make teardown kill the recorded PID, and parameterize the origin in `auth.setup.ts` from `SKILLING_SERVER_URL`.

### Resolution

`globalSetup.ts` now seeds `e2e/test-data/` into `run/plugins/Skilling/` (wiping stale runtime state), spawns `./gradlew runServer --no-daemon` as a detached process group, records the wrapper PID to `.server.pid`, and waits for `/api/health` (failing fast if the server exits early). `globalTeardown.ts` kills the recorded process group AND scans `/proc` for the run-paper server JVM (forked into its own group, so group-killing the wrapper alone orphaned it). `auth.setup.ts` and `playwright.config.ts` honor `SKILLING_SERVER_URL` for external mode.

Three blockers found and fixed while validating end-to-end:
- **E2E fixture `tags.yml` used `c:`-prefixed keys** (the loader auto-prefixes), which made `CustomTagLoader` throw on `#c:ores` and prevented the plugin from enabling. The fixture now mirrors the canonical resource tag set with bare keys.
- **Shipped default `tags.yml` referenced `#minecraft:tall_flowers`**, which does not exist as a tag in 1.21.8 — a fresh install could not enable the plugin. Removed the reference from `src/main/resources/tags.yml` (verified against the Paper jar's tag data; all other referenced tags exist).
- **The plugin rotates the default `web.password` (`skilling`)** on first enable, so the fixed fixture password could never authenticate. The fixture now uses a non-default password (`e2e_secret`, overridable via `SKILLING_WEB_PASSWORD` for external servers), and all e2e auth/credentials were centralized in `helpers/credentials.ts`.

Two latent spec bugs (never run before, since automatic mode didn't exist) were also fixed: the lore-preview color/format assertions now use computed CSS instead of raw style-attribute substrings (Vue serializes `color:#FF5555` as `rgb(255, 85, 85)`), and the cooldown test now verifies numeric load/edit/save without assuming staged edits are visible through the live-only GET.

## Verification & Definition of Done

- [x] `npm run e2e` (automatic mode) passes on a clean checkout without a pre-running server — **71/71 tests pass, server bootstrapped, fixtures seeded, teardown stops the server**
- [x] External-server mode (`SKILLING_SERVER_URL=... npm run e2e`) still passes — verified 23 tests against a manually started server; the external server is left running
- [x] Teardown reliably stops the started server — verified: `Stopped server processes: <wrapper>, <server-jvm>` and no orphaned JVM remains
