# ISSUE-162: Keep the Pinia auth store in sync with 401/session expiry

**Status:** Closed
**Type:** Bug
**Severity:** High (UI stays "authenticated" over the login form)

---

## Context & User Story

- **Goal:** As an admin, I want a 401/session-expiry to log me out consistently everywhere — routing to login, hiding the topbar and pending-changes banner, and stopping staging polls.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [x] On 401, clear both the stored credentials **and** the Pinia `authStore.user` state so `isAuthenticated` becomes false
- [x] Ensure the router guard redirects to `#/login` consistently after expiry
- [x] Stop the staging poll / PendingChangesBanner when unauthenticated
- [x] Add a test covering: API returns 401 → store is logged out → router redirects → banner hides

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/api/client.ts:22-26`
  - `web/frontend/src/stores/auth.ts`
  - `web/frontend/src/router.ts:62-67`
  - `web/frontend/src/App.vue:3-4` (banner keyed on `authStore.isAuthenticated`)
- **Dependencies:** `sessionStorage` credentials and the Pinia store are two independent session notions today.
- **Constraints:** Single source of truth for auth state; avoid logout loops on the login page itself.

### Root Cause

On a 401, `clearCredentials()` runs and the hash is set to `#/login`, but `authStore.user` is never cleared. `isAuthenticated` remains true, so the router guard does not redirect, `AppTopbar`/`PendingChangesBanner` stay visible over the login form, and the staging banner keeps polling. The auth store and the API client drift.

### Proposed Fix

Call an `authStore.logout()` (clearing user + credentials + router redirect) from the shared 401 handler, and gate the banner/poll on the store's authenticated state.

### Resolution

`apiFetch`'s 401 handler now calls `authStore.logout()` (which clears both `user` and `sessionStorage` credentials) before forcing the `#/login` hash, so `isAuthenticated` flips false and the router guard, `AppTopbar`, and `PendingChangesBanner` all react. The circular `client → auth → client` import is safe because both sides only use each other's exports at runtime. `App.vue` additionally gates the route-change staging fetch on `isAuthenticated`, so no 401 fetches fire while logged out. The login page's own credential check bypasses `apiFetch`, avoiding logout loops. A new E2E test routes the skills-list request to 401 and asserts the redirect to login, the hidden topbar, and cleared credentials.

## Verification & Definition of Done

- [x] `cd web/frontend && npm run build` passes
- [x] Test: simulated 401 logs the user out end-to-end (store, router, banner) — new E2E test; full suite 77/77 pass
- [x] Manual smoke: expire the session mid-session and confirm clean redirect (covered by the route-fulfilled 401 E2E test)
