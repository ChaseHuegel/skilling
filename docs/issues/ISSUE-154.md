# ISSUE-154: Redact the web password from API responses and make web credential/port changes take effect

**Status:** Resolved
**Type:** Bug
**Severity:** High (credential exposure + stale-credential security hazard)

---

## Context & User Story

- **Goal:** As an admin, I want the web GUI to never echo my password back to me, and when I change the web password or port I want the change to actually take effect.
- **Agent Role:** You are an expert security/backend engineer executing this task.

## Implementation Requirements

- [x] Redact `web.password` from `GET /api/config` (return a placeholder or omit the field) so the stored credential is not exposed to any authenticated client or reflected back
- [x] Make a staged `web.password`/`web.port` change actually restart/reconfigure the embedded server (or clearly reject such keys as apply-requires-restart)
- [x] Ensure the frontend still works when the password field is redacted (edit UX: blank = keep current)
- [x] Add a test covering: config response does not contain the plaintext password; password/port changes either take effect or are explicitly rejected

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ConfigHandler.java:60`
  - `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java:36` (authenticator/port snapshot from startup)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:601-608` (`reloadConfigSettings` doesn't touch web settings)
  - `src/main/java/io/github/chasehuegel/skilling/web/config/WebConfig.java`
- **Dependencies:** ISSUE-155 (auth hardening) is related but separate.
- **Constraints:** Do not break the config round-trip for non-secret keys.

### Root Cause

`GET /api/config` includes `web.password` (and username) in plaintext — the same credential used for auth. Additionally, a staged `web.password`/`web.port` change is applied to `config.yml` and reloaded, but `WebConfig`/`BasicAuthenticator`/the bound port are immutable snapshots from `onEnable`. The admin believes credentials were rotated; the old password keeps working and the new one never does.

### Proposed Fix

Omit the password from the config DTO (frontend treats blank as unchanged). On apply, either reconfigure the authenticator/bind live or reject password/port changes with a clear "requires server restart" message in both the web UI and `/skills set`.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new regression test
- [x] Test: `GET /api/config` response contains no plaintext `web.password`
- [x] Test: changing the web password either takes effect on next request or is explicitly rejected with guidance
- [x] Frontend build (`cd web/frontend && npm run build`) passes with the redacted password field
