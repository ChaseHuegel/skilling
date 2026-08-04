# ISSUE-218: Web GUI config/data-loss and auth hardening

## Context & User Story
- **Goal:** As a server admin, I want web config saves to never drop config keys I did not edit, and the admin API to stay usable and secure when deployed behind the recommended reverse proxy.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] **Config whitelist data loss:** `ConfigHandler.update` rebuilds `config.yml` from a ~10-key whitelist (`ConfigHandler.java:87-147`), dropping every unknown key on apply (including `setup.first_run` and any admin/plugin-added or future keys). Round-trip the full live config and update only the edited keys, or merge with the existing file so unknown keys are preserved.
- [x] **Rate-limiter proxy collapse:** `WebServer.java:85-86` keys `AuthRateLimiter` on `ctx.ip()` (the socket address). Behind the recommended reverse proxy every client appears as the proxy IP, so 10 failures by anyone lock out everyone (including the admin) for 15 minutes. Resolve the real client IP from `X-Forwarded-For` (with a trusted-proxy config), or otherwise scope the limiter so a proxy deployment cannot produce a global lockout.
- [x] **CORS + Basic auth review:** `WebServer.java:73` sends `Access-Control-Allow-Origin: *` on every response while the API uses Basic auth. Confirm whether any cross-origin page can trigger or read admin API calls with cached credentials; if so, restrict the allowed origin to the configured frontend origin and/or add a CSRF-style guard on state-changing routes.
- [x] Add regression tests: a config update round-trip preserves unknown keys; a rate limiter unit test covering a spoofed/forwarded IP (existing `AuthRateLimiterTest` and `ConfigHandlerSecurityTest` to extend).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ConfigHandler.java`
  - `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java`
  - `src/main/java/io/github/chasehuegel/skilling/web/auth/AuthRateLimiter.java`
  - `src/main/resources/config.yml` (reverse-proxy note in `web:` docs)
  - Tests: `ConfigHandlerSecurityTest`, `AuthRateLimiterTest`
- **Dependencies:** none.
- **Constraints:** Port/username/password changes correctly require a restart (already enforced). Keep the generated-password and credential-redaction behaviors.

## Verification & Definition of Done
- [x] A config save round-trip preserves all keys not present in the editor payload.
- [x] A brute-force burst behind a proxy cannot lock out all clients; per-client IPs still get rate limited.
- [x] CORS policy does not allow unauthorized cross-origin reads/writes with cached admin credentials.
- [x] `./gradlew build` and `./gradlew test` pass.
