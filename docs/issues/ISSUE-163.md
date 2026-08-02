# ISSUE-163: Implement the E2E "automatic mode" (server bootstrap + fixture seeding)

**Status:** Open
**Type:** Bug
**Severity:** High (documented automatic mode does not exist; `npm run e2e` fails without a manual server)

---

## Context & User Story

- **Goal:** As a developer, I want `npm run e2e` to start the Paper dev server, seed fixture data, run the tests, and stop the server — exactly as `web/AGENTS.md` documents.
- **Agent Role:** You are an expert QA/frontend engineer executing this task.

## Implementation Requirements

- [ ] Make `globalSetup.ts` copy `e2e/test-data/` fixtures into `run/plugins/Skilling/` and start the Paper dev server (`./gradlew runServer`) before tests run
- [ ] Record the server PID so `globalTeardown.ts` actually stops it (currently it reads `../.server.pid` that nothing writes)
- [ ] Preserve the `SKILLING_SERVER_URL` external-server mode as an alternative
- [ ] Fix `auth.setup.ts` to honor `SKILLING_SERVER_URL` (currently hardcodes `http://localhost:8082`)
- [ ] Verify `npm run e2e` works end-to-end without a manually running server

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

## Verification & Definition of Done

- [ ] `npm run e2e` (automatic mode) passes on a clean checkout without a pre-running server
- [ ] External-server mode (`SKILLING_SERVER_URL=... npm run e2e`) still passes
- [ ] Teardown reliably stops the started server
