# ISSUE-227: Refresh stale frontend fallback registries

## Context & User Story
- **Goal:** As a server admin, I want the web editor's offline fallback suggestions to match the actual registered mechanics, triggers, and state filters so a suggestion never leads to content the engine rejects or ignores.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements
- [ ] Refresh `FALLBACK_MECHANICS` in `MechanicsEditor.vue:31-36` to match the live registry keys registered in `Skilling.registerBuiltinMechanics` (`Skilling.java:286-381`). Today it is a stale subset and lists non-registered keys (e.g. `core:cancel_damage` — the registry has `core:block_damage` and `core:dodge`; `core:modify_damage` is registered but the entry below is stale).
- [ ] Refresh `FALLBACK_PARAM_NAMES` (`MechanicsEditor.vue:42-63`) to the actual `List.of(...)` parameter names from `registerBuiltinMechanics` (e.g. `core:chain_break` → `["chain_limit"]`, drop `exhaustion`; add missing mechanics such as `core:level_break`, `core:auto_smelt`, `core:xp_bonus`, `core:fishing_yield`, `core:area_harvest`, `core:field_aura`, `core:block_particles`, etc.).
- [ ] Refresh `FALLBACK_TRIGGERS` in `AbilitiesSection.vue:34-40` and `XpSourcesSection.vue:79-84` to the registered trigger keys (`Skilling.java:386-415`); today they are missing `brew_start`, `repair`, and use inconsistent names (`player_shear`/`player_tame` are correct in one list but the other list omits several keys).
- [ ] Refresh `STATE_SUGGESTIONS` in `stateFilters.ts` to the registered state filters (`Skilling.java:421-560`): add `target_type` (with a `#c:` example), `biome`, and confirm the `equipped_all`/`equipped_any` forms and value formats match the engine's parsing.
- [ ] Consider removing the fallback lists entirely (treat a failed registries fetch as a degraded-but-honest empty suggestion list) if that is simpler than maintaining two sources of truth.

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/components/skills/MechanicsEditor.vue`
  - `web/frontend/src/components/skills/AbilitiesSection.vue`
  - `web/frontend/src/components/skills/XpSourcesSection.vue`
  - `web/frontend/src/components/common/stateFilters.ts`
  - Reference: `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (`registerBuiltinMechanics`, `registerBuiltinTriggers`, `registerBuiltinStateFilters`)
- **Dependencies:** none. The live `/api/mechanics`, `/api/triggers`, and `/api/state-filters` endpoints are the source of truth; the fallbacks only render when those fail.
- **Constraints:** The fallback lists must stay in sync with `Skilling.java` registrations — prefer removing them or generating them from a shared source rather than re-syncing by hand again.

## Verification & Definition of Done
- [ ] Every fallback mechanic key is registered in `Skilling.java`; every fallback parameter name appears in the matching mechanic's registered `List.of(...)`.
- [ ] Fallback trigger keys match the registered trigger set exactly.
- [ ] Fallback state filters match the registered state-filter keys (including `target_type` and `biome`).
- [ ] `cd web/frontend && npm run build` passes.
