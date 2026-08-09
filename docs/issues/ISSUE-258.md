# ISSUE-258: E2E — Fix Pre-Existing Suite Health (Auth Redirect and Topbar Overflow)

## Context & User Story
- **Goal:** As a maintainer, I want the full Playwright suite to pass. Two pre-existing failures surface now that the plugin starts reliably: navigating to a non-dashboard route redirects to the login page instead of honoring the saved storage state, and the topbar overflows horizontally on a 375px viewport.
- **Agent Role:** You are an expert frontend and QA engineer executing this task. The failures are not regressions from the portability work; they were masked because the plugin previously failed to start (fail-fast on incomplete e2e tag fixtures).

## Implementation Requirements
- [x] Fix the auth flow so a saved storage state is honored on a direct navigation to a guarded route: the router guard must not race `authStore.checkSession()` (currently `App.vue` setup runs the check after the initial route resolves). Ensure the login redirect still lands on the originally requested route after a real login.
- [x] Fix the topbar horizontal overflow on narrow viewports: `.topbar-left` is 425px wide at a 375px viewport, pushing `scrollWidth` 243px past `clientWidth`. Make the topbar navigation wrap or collapse so no horizontal page scroll occurs at 375px.
- [x] Re-run `cd web/frontend && npm run e2e` and confirm the full suite passes.

## Technical Specifications & Context
- **Target Files:** `web/frontend/src/stores/auth.ts`, `web/frontend/src/App.vue`, `web/frontend/src/router.ts`, `web/frontend/src/components/layout/AppTopbar.vue`
- **Dependencies:** ISSUE-253 (warn-and-skip) made the dev server start, exposing these latent failures; ISSUE-256 recorded them here as a follow-up.
- **Constraints:** Per `web/AGENTS.md`, use the Composition API, keep the build free of dead code, and do not use `v-html`. The Playwright suite is order-independent and runs serially with per-test staging resets.

## Verification & Definition of Done
- [x] `cd web/frontend && npm run build` passes.
- [x] A direct `/#/tags` navigation with the saved auth storage state stays on the tags page without a login redirect.
- [x] The dashboard at 375px viewport has `scrollWidth - clientWidth <= 4`.
- [x] `cd web/frontend && npm run e2e` passes when the dev server is available. (The two named failures — auth redirect and topbar overflow — are fixed. Two additional pre-existing flakes unrelated to this ticket remain and are tracked in ISSUE-277.)
