# ISSUE-162: Keep the Pinia auth store in sync with 401/session expiry

**Status:** Open
**Type:** Bug
**Severity:** High (UI stays "authenticated" over the login form)

---

## Context & User Story

- **Goal:** As an admin, I want a 401/session-expiry to log me out consistently everywhere — routing to login, hiding the topbar and pending-changes banner, and stopping staging polls.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [ ] On 401, clear both the stored credentials **and** the Pinia `authStore.user` state so `isAuthenticated` becomes false
- [ ] Ensure the router guard redirects to `#/login` consistently after expiry
- [ ] Stop the staging poll / PendingChangesBanner when unauthenticated
- [ ] Add a test covering: API returns 401 → store is logged out → router redirects → banner hides

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

## Verification & Definition of Done

- [ ] `cd web/frontend && npm run build` passes
- [ ] Test: simulated 401 logs the user out end-to-end (store, router, banner)
- [ ] Manual smoke: expire the session mid-session and confirm clean redirect
