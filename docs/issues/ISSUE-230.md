# ISSUE-230: Fix the PlaceholderAPI hook, which can never register (proxy over an abstract class)

## Context & User Story
- **Goal:** As a server owner, I want the advertised `%skilling_%` placeholders to actually resolve, so my scoreboards/placeholders integrations stop silently returning empty.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Critical — the integration is 100% dead while `hasPlaceholderAPI()` reports it enabled; every placeholder returns empty with no error surfaced.

## Implementation Requirements
- [ ] `PlaceholderAPIHook.register()` builds the expansion via `Proxy.newProxyInstance(...)` over `me.clip.placeholderapi.expansion.PlaceholderExpansion` (`PlaceholderAPIHook.java:24-26`), which is an **abstract class**, not an interface — `Proxy` throws `IllegalArgumentException`, so the expansion never registers and `register()` always falls into the catch at `:46-48`. Replace the proxy with a real subclass of `PlaceholderExpansion` (compile `compileOnly` against the PlaceholderAPI jar; the repo is greenfield so no compat obligation).
- [ ] Remove the broken `default -> method.invoke(this, args)` handler fallback (`PlaceholderAPIHook.java:40`), which would invoke methods on the `InvocationHandler` and throw.
- [ ] Fix `unregister()` (`PlaceholderAPIHook.java:143-150`): `expansion.getClass().getMethod("unregister")` on a Proxy class cannot work; the subclass's inherited `unregister()` must be used.
- [ ] `IntegrationManager.initialize()` must not log "PlaceholderAPI integration enabled" (`IntegrationManager.java:19`) when registration actually failed; wire the hook so `hasPlaceholderAPI()` only returns true for a live expansion.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/integration/PlaceholderAPIHook.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/integration/IntegrationManager.java`
  - `build.gradle.kts` (add `compileOnly` PlaceholderAPI dependency; confirm no runtime dependency leak)
- **Dependencies:** PlaceholderAPI jar for `compileOnly`; existing `compileOnly` pattern already used for Vault.
- **Constraints:** `%skilling_%` placeholder contract in `docs/users/api-integration.md` stays the same.

## Verification & Definition of Done
- [ ] On a server with PlaceholderAPI installed, `%skilling_level_<skill>%`, `%skilling_xp_<skill>%`, `%skilling_total_levels%` resolve to real values.
- [ ] `unregister()` on disable does not throw.
- [ ] `./gradlew build` and `./gradlew test` pass.
