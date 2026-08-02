# ISSUE-164: Improve E2E isolation and make screenshot assertions real

**Status:** Open
**Type:** Improvement
**Severity:** Medium (shared mutable state, non-asserting screenshots, fixed sleeps)

---

## Context & User Story

- **Goal:** As a developer, I want the E2E suite to be reliable in isolation, order-independent, and to actually assert on visual output.
- **Agent Role:** You are an expert QA engineer executing this task.

## Implementation Requirements

- [ ] Isolate tests from shared mutable server state: reset/re-seed pending changes and skill data per spec (or per test) instead of relying on serial file ordering and alphabetical worker scheduling
- [ ] Replace `takeScreenshot()`-only calls with real `toHaveScreenshot()` assertions where visual checks are intended (or remove the screenshot calls and their dead baseline config)
- [ ] Replace fixed sleeps (`shared-login.ts:11`, `GuiLayoutPage.ts:49,79,84`) with auto-retrying assertions / `expect.poll`
- [ ] Remove the extra `/#/` navigation + 500ms from every POM `goto()`
- [ ] Add fixtures isolation strategy so a mid-suite failure does not cascade (banners/cooldown state)

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/e2e/specs/{skill-editor,staged-changes,ability-trigger,dashboard,config,tags,gui-layout,responsive}.spec.ts`
  - `web/frontend/e2e/pages/{shared-login,GuiLayoutPage}.ts`
  - `web/frontend/e2e/helpers/debug.ts:90-98`
  - `web/frontend/e2e/playwright.config.ts:5-13`
- **Dependencies:** ISSUE-163 (server bootstrap) so tests can re-seed via the plugin API or fixtures.
- **Constraints:** Tests must be deterministic and order-independent.

### Root Cause

The suite shares mutable server state: specs stage/delete skills and depend on serial, alphabetical file ordering (`workers: 1`, `fullyParallel: false`). Any mid-suite failure leaves pending changes that cascade. Screenshot tests call `takeScreenshot()` which only saves a PNG — zero `toHaveScreenshot()` assertions exist, so `e2e:update`/baseline config is dead weight and these tests always pass. Fixed sleeps in POMs and an extra navigation+wait in every `goto()` slow the suite and add flakiness.

### Proposed Fix

Introduce per-spec setup that resets staging and restores fixture skill state (via API or re-seeding), replace sleeps with retrying assertions, and either add real screenshot comparisons or drop the un-asserting screenshot calls.

## Verification & Definition of Done

- [ ] `npm run e2e` passes with `workers > 1` / shuffled order (order-independent)
- [ ] Screenshot tests either assert via `toHaveScreenshot()` or are removed with their baseline config
- [ ] No fixed `waitForTimeout` sleeps remain in POMs
