# ISSUE-138: Return `PlayerProfileView` from the public API instead of the mutable `PlayerProfile`

**Status:** Open
**Type:** Bug
**Severity:** High (shipped read-only contract is dead; API leaks mutation surface)

---

## Context & User Story

- **Goal:** As an addon developer, I want the API to hand me a read-only view of a player's profile, so I cannot accidentally corrupt live state.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Change `SkillingAPI.getProfile(UUID)` to return `PlayerProfileView` (the shipped read-only contract)
- [ ] Keep `PlayerProfile implements PlayerProfileView` so engine code is unaffected
- [ ] If engine internals need the mutable profile, expose a separate non-public path or document it; addons only ever see the view
- [ ] Ensure the view's `getXp`/`getXpSnapshot`/`isInitialized` methods have the required Javadoc (`skilling-api/AGENTS.md`)
- [ ] Update `docs/users/api-integration.md` (`getProfile` → `PlayerProfile` reference) to the view type
- [ ] Add a test asserting the API returns a type that cannot mutate XP

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/api/SkillingAPI.java:102-104`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/profile/PlayerProfileView.java:12-20`
  - `src/main/java/io/github/chasehuegel/skilling/engine/profile/PlayerProfile.java:24`
  - `docs/users/api-integration.md:317`
- **Dependencies:** Binary-compatibility review (a signature change is a minor/major bump per `CONVENTIONS-COMMITS.md`).
- **Constraints:** `PlayerProfileView` is referenced exactly once today (the `implements` clause); this issue makes it the API contract.

### Root Cause

`PlayerProfileView` was shipped as the read-only view contract but no API method returns it — `SkillingAPI.getProfile` returns the concrete `PlayerProfile`, which exposes `setXp`, `addXp`, `setPreferences`, etc. Addons can mutate live profile state that the engine assumes it owns.

### Proposed Fix

Change the return type to `PlayerProfileView` and keep the concrete class internal-only. Add a clearly-named engine-internal accessor if the web backend or commands need mutability.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including a new API-contract test
- [ ] Unit test: `SkillingAPI.getProfile` return type is `PlayerProfileView` (compile-time assertion)
- [ ] `docs/users/api-integration.md` documents the view type and what it exposes
- [ ] Engine/web code that needs mutation uses the internal accessor
