# SKILL-DEVELOPMENT-PLAN — Phase 2: Namespaced IDs, `core:ally_aura`, `state:equipped`

**Review references:** P2-5, P2-6, P2-11, C5, C6, C8, C12 (§3.1 of `SKILL-REVIEW.md`)
**Priority:** P1 — Pillar violations and correctness fixes needed before Phase 4 YAMLs can load
**Estimated commits:** 5

---

## Objective

1. **P2-11:** Migrate the `effect` and `attribute` parameters from fragile legacy numeric IDs to namespaced keys (`minecraft:regeneration`), with backward-compatible numeric fallback + deprecation warning. Fail-fast on unknown keys per `AGENTS.md`.
2. **P2-5:** Implement `core:ally_aura` — a player-only AoE buff mechanic that replaces the broken `core:field_aura` usage in Bard/Piety (which currently buffs hostile mobs).
3. **P2-6:** Implement `state:equipped` filter — verifies the armor *type* worn (light/medium/heavy/none) so armor skills gate XP and abilities to the correct gear.
4. Add a `/api/state-filters` REST endpoint so the Web GUI can discover state filter keys dynamically (closing the hardcoded-list gap identified in the web exploration).
5. Update frontend suggestion lists and documentation.

---

## Prerequisites

- Phase 1 complete (trigger field exists, `fireAbilities()` enforces it)
- `SKILL-REVIEW.md` Part 2 (P2-5, P2-6, P2-11)
- `AGENTS.md` §5 (fail-fast), §1 (composition-over-inheritance)

---

## Commit 2.1 — Migrate `PotionEffectResolver` to namespaced keys (P2-11a)

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/PotionEffectResolver.java` | 1. Add a `resolve(Object raw)` overload that accepts a `String` namespaced key (e.g., `"minecraft:regeneration"`) and calls `Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(key))`. 2. If the input is numeric, resolve via the existing legacy array but log a deprecation `WARNING`. 3. If the resolved type is `null`, throw `IllegalArgumentException` (fail-fast per `AGENTS.md`). 4. Keep the existing `LEGACY_IDS`/`LEGACY_NAMES` arrays for backward compat. |

**Validation:** `./gradlew build && ./gradlew test`

**Test:** Add `PotionEffectResolverTest`:
- Namespaced key `"minecraft:speed"` → `PotionEffectType.SPEED`
- Namespaced key `"minecraft:poison"` → `PotionEffectType.POISON`
- Unknown key `"minecraft:nonexistent"` → `IllegalArgumentException`
- Legacy numeric `19` → `PotionEffectType.POISON` + deprecation warning (verify via log capture)
- Null input → `IllegalArgumentException`

```
feat(engine): accept namespaced effect keys in PotionEffectResolver

PotionEffectResolver now accepts namespaced keys (e.g.
"minecraft:poison") in addition to legacy numeric IDs. Numeric
inputs trigger a deprecation warning. Unknown keys throw
IllegalArgumentException per the fail-fast convention. This
eliminates the fragile hardcoded ID→name array as the sole
resolution path.
```

---

## Commit 2.2 — Migrate `ModifyAttributeMechanic` to namespaced keys (P2-11b)

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/ModifyAttributeMechanic.java` | 1. If `params.get("attribute")` is a `String` matching `minecraft:<key>`, resolve via `Registry.ATTRIBUTE.get(NamespacedKey.fromString(key))`. 2. If numeric, fall back to the existing `switch(id)` with a deprecation warning. 3. If the resolved attribute is `null`, throw `IllegalArgumentException`. 4. Update class-level Javadoc to document both forms. |

**Validation:** `./gradlew build && ./gradlew test`

**Test:** Add `ModifyAttributeMechanicTest` (requires mocking `Player.getAttribute()` — use a test fixture or verify resolution logic in isolation):
- Namespaced key `"minecraft:movement_speed"` → `Attribute.MOVEMENT_SPEED`
- Legacy numeric `4` → `Attribute.MOVEMENT_SPEED` + deprecation warning
- Unknown key → `IllegalArgumentException`

```
feat(engine): accept namespaced attribute keys in ModifyAttributeMechanic

ModifyAttributeMechanic now accepts namespaced attribute keys
(e.g. "minecraft:movement_speed") in addition to legacy numeric
IDs. Numeric inputs trigger a deprecation warning. Unknown keys
throw IllegalArgumentException per the fail-fast convention.
```

---

## Commit 2.3 — Implement `core:ally_aura` mechanic (P2-5)

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/AllyAuraMechanic.java` | New class implementing `SkillMechanic`. Behavior: resolve effect via `PotionEffectResolver.resolve(params.get("effect"))`, apply to `player` and all `player.getLocation().getNearbyPlayers(radius)`. Return `true` on success. Class-level Javadoc per `AGENTS.md` (purpose, YAML key, parameters: `effect`, `radius`, `duration`, `amplifier`). |
| `src/main/java/.../Skilling.java` | Register: `mechReg.register("core:ally_aura", AllyAuraMechanic.class, List.of("effect", "radius", "duration", "amplifier"));` |

**Validation:** `./gradlew build && ./gradlew test`

**Design notes (from `SKILL-REVIEW.md` P2-5):**
- Unlike `core:field_aura`, this iterates `getNearbyPlayers()` only — never `getNearbyLivingEntities()`, so hostile mobs are never buffed.
- The mechanic itself has no item-cost logic; item consumption is handled by the ability's `requirements.items` with `action: cost` (the Check/Execute/Consume pattern per `AGENTS.md` §3).
- This mechanic serves as the replacement for all `core:field_aura` usages in Bard and Piety redesigned skills (Phase 4).

```
feat(engine): add core:ally_aura mechanic for player-only AoE buffs

AllyAuraMechanic applies a potion effect to the player and nearby
players only (not hostile mobs). This replaces the broken
core:field_aura usages in Bard and Piety, which inadvertently
buffed all nearby LivingEntity including enemies. Item economy
is enforced via the ability requirements.items cost action.
```

---

## Commit 2.4 — Implement `state:equipped` state filter (P2-6)

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../Skilling.java` | In `registerBuiltinStateFilters()`, add: `sf.register("equipped", (p, e, v) -> { ... })`. The lambda reads `p.getInventory().getArmorContents()`, maps each `ItemStack` Material to a tier (`light`=leather, `medium`=chain/iron/gold/turtle, `heavy`=diamond/netherite, `none`=air/empty), and requires all four slots to match the requested tier. |

**Validation:** `./gradlew build && ./gradlew test`

**Design notes (from `SKILL-REVIEW.md` P2-6):**
- Value syntax: `equipped:light`, `equipped:medium`, `equipped:heavy`, `equipped:none`
- A mismatched set (e.g., leather helmet + diamond chest) fails — all four must match.
- `equipped:none` is equivalent to the existing `armor:empty` filter but expressed consistently within the `equipped` namespace.
- This filter is used by `light_armor`, `medium_armor`, `heavy_armor` skills (Phase 4 redesigns) to gate XP and abilities to the correct armor type.

**Test:** Add `StateFilterEquippedTest`:
- Full leather set → `equipped:light` passes, `equipped:heavy` fails
- Full diamond set → `equipped:heavy` passes, `equipped:light` fails
- Mixed set → all `equipped:*` fail
- Empty slots → `equipped:none` passes

```
feat(engine): add state:equipped filter for armor-type gating

The equipped state filter verifies that all four armor slots match
the requested tier (light/medium/heavy/none). This gates armor-skill
XP and abilities to the correct gear type, preventing Heavy Armor
from granting XP while wearing leather.
```

---

## Commit 2.5 — Web GUI parity: `/api/state-filters` endpoint + frontend updates

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../web/WebServer.java` | Add route `GET /api/state-filters` that calls `plugin.getStateFilterRegistry()` and serializes the registered keys to JSON: `{ "stateFilters": ["is_sneaking", "is_sprinting", ..., "equipped", ...] }`. |
| `src/main/java/.../web/handler/StateFilterHandler.java` (new) | Handler class: `list(ctx)` returns the state filter keys as a JSON array. |
| `web/frontend/src/api/client.ts` | Add: `stateFilters: { list: () => apiFetch<{ stateFilters: string[] }>('/api/state-filters') }` |
| `web/frontend/src/stores/registries.ts` | Fetch state filters in the same `Promise.all` as mechanics/triggers. Store in `stateFilters` ref. |
| `web/frontend/src/components/common/FilterBuilder.vue` | Replace hardcoded `STATE_SUGGESTIONS` with a computed that uses `registriesStore.stateFilters` (dynamic), falling back to the existing hardcoded list (now exported from Commit 1.4's shared constant) if the API call failed. |
| `web/frontend/src/components/skills/AbilitiesSection.vue` | Same: replace `STATE_OPTIONS` with the dynamic store value + shared fallback. (The shared constant from Commit 1.4 ensures both sites use the same fallback.) |

**Validation:** `./gradlew build`, `cd web/frontend && npm run build && npm run e2e`

**Test:** Add E2E spec verifying that state filter suggestions include the new `equipped` key when the API is available.

```
feat(web): add /api/state-filters endpoint for dynamic filter discovery

The Web GUI now discovers state filter keys dynamically via
GET /api/state-filters, eliminating the need to manually update
hardcoded suggestion lists when new StateFilter implementations
are registered. Both FilterBuilder and AbilitiesSection consume
the dynamic list with a shared fallback constant.
```

---

## Phase 2 Completion Checklist

- [ ] `PotionEffectResolver` accepts namespaced keys, deprecates numeric, fails-fast on unknown
- [ ] `ModifyAttributeMechanic` accepts namespaced keys, deprecates numeric, fails-fast on unknown
- [ ] `core:ally_aura` registered and applies to players only
- [ ] `state:equipped` filter registered and validates armor type across all 4 slots
- [ ] `/api/state-filters` endpoint exposes live state filter keys
- [ ] Frontend FilterBuilder and AbilitiesSection use dynamic state filter suggestions
- [ ] `./gradlew build` passes
- [ ] `./gradlew test` passes (new tests for resolver, mechanic, state filter)
- [ ] `cd web/frontend && npm run build` passes
- [ ] E2E tests pass

## Dependencies on Later Phases

- Phase 4 redesigned skills use `minecraft:poison`, `minecraft:slowness`, etc. (namespaced effect keys) and `state:equipped:light/medium/heavy` filters and `core:ally_aura` mechanics.
- Phase 5 documentation catalog will document the new keys, mechanic, and filter.