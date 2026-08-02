# ISSUE-155: Harden web authentication (defaults, rate limiting, constant-time compare)

**Status:** Resolved
**Type:** Bug
**Severity:** High (publicly-known default credentials, no brute-force protection)

---

## Context & User Story

- **Goal:** As a server owner, I want the admin web interface to be reasonably protected from brute-force and offline-guessing attacks when I enable it.
- **Agent Role:** You are an expert security engineer executing this task.

## Implementation Requirements

- [x] Generate a random admin password on first enable (or force a password change) instead of shipping known `admin`/`skilling` defaults
- [x] Add rate limiting / lockout on `/api/auth/check` (and all auth attempts) to slow brute force
- [x] Make the credential comparison constant-time to avoid a timing side channel
- [x] Document the risk of Basic auth over plaintext HTTP (or support an optional HTTPS/bind-to-localhost hardening flag)
- [x] Add tests covering: constant-time compare behavior, rate-limit triggers after N failures

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/config/WebConfig.java:11-18` (defaults)
  - `src/main/resources/config.yml:52-59`
  - `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java:91-104`
  - `src/main/java/io/github/chasehuegel/skilling/web/auth/BasicAuthenticator.java:15-29`
- **Dependencies:** None beyond Javalin/Jetty.
- **Constraints:** `web.enabled` stays off by default. Keep the auth filter fast on the success path.

### Root Cause

`web.enabled` is off by default, but enabling it with shipped defaults gives credentials `admin`/`skilling` (publicly known), sent as trivially decodable base64 over plaintext HTTP, with no rate limiting or lockout on `/api/auth/check` and a non-constant-time comparison — a timing side channel. Anyone on the network can brute-force.

### Proposed Fix

On first enable (no configured password), generate and log a random password. Add an in-memory attempt counter per client IP (lockout after N failures) and use a constant-time comparison (e.g. `MessageDigest.isEqual`).

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new auth tests
- [x] Test: comparison is constant-time (no early-exit timing observable)
- [x] Test: after N failed attempts, `/api/auth/check` returns 429/lockout for that client
- [x] First-enable flow logs a generated password and stores it hashed/configured
