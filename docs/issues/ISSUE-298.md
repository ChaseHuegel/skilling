# ISSUE-298: Web backend security hardening — origin checks, auth timing, and resource bounds

## Context & User Story
- **Goal:** As a maintainer, I want the web admin console hardened against cross-origin state changes, auth timing side channels, and memory-exhaustion floods.
- **Agent Role:** You are an expert backend/security engineer executing this task.

## Implementation Requirements
- [ ] **CORS origin must block state-changing requests:** `WebServer.java:75-118` only decides whether to reflect `Access-Control-Allow-Origin`; a request with a disallowed `Origin` is still fully processed, so `POST /api/reload` and `DELETE /api/staging` can be triggered cross-site when the browser sends valid credentials. In the `before` handler, return 403 for `POST`/`PUT`/`DELETE` requests carrying an `Origin` that `originAllowed` rejects (and keep `OPTIONS` preflight correct).
- [ ] **401 responses must include `WWW-Authenticate`:** `WebServer.java:110-113,136-139` returns a bare JSON 401. Emit `WWW-Authenticate: Basic realm="skilling"` per RFC 7617.
- [ ] **Constant-time auth comparison:** `BasicAuthenticator.java:29,41-47` short-circuits on username mismatch and `MessageDigest.isEqual` early-returns on length mismatch, leaking username/length via timing. Compare concatenated username+password (or padded fixed-length buffers) so equal-length inputs take constant time.
- [ ] **Bound the auth rate-limiter map:** `AuthRateLimiter.java:83-87` only prunes entries older than the window+block cutoff, so a multi-IP flood with `windowStart ≈ now` grows the map without bound. Add a hard size cap that evicts an arbitrary (oldest) entry when full.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java:75-118,136-139,253-258`, `src/main/java/io/github/chasehuegel/skilling/web/auth/BasicAuthenticator.java:29,41-47`, `src/main/java/io/github/chasehuegel/skilling/web/auth/AuthRateLimiter.java:83-87`. Tests: `src/test/java/io/github/chasehuegel/skilling/web/auth/*`, `WebServerClientIpTest.java`.
- **Dependencies:** None.
- **Constraints:** The frontend reads credentials from `sessionStorage`; do not break the normal in-app login flow or Playwright auth. Keep `X-Forwarded-For` handling as configured by `behind_proxy`.

## Verification & Definition of Done
- [ ] New tests: disallowed-Origin POST returns 403; 401 carries `WWW-Authenticate`; rate-limiter map stays bounded under a many-IP burst.
- [ ] Existing auth tests still pass.
- [ ] `./gradlew test` and `./gradlew build` pass.
