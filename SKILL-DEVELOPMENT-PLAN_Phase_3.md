# SKILL-DEVELOPMENT-PLAN — Phase 3: New Engine Mechanics & Triggers

**Review references:** P2-2, P2-3, P2-4, P2-7, P2-8, P2-9, P2-10, P2-12 (§Part 2 of `SKILL-REVIEW.md`)
**Priority:** P3 — Must land before Phase 4 YAMLs that reference these keys can load
**Estimated commits:** 9

---

## Objective

Implement and register the seven new mechanics, two new triggers, and one schema enhancement that the Phase 4 redesigned skills reference. Each piece satisfies Pillars I–V (`SKILL-DESIGN-FRAMEWORK.md` §1) and the 6-Step Checklist (§5): no per-tick runnables, no NMS, no packet spam, O(1) event listeners, vanilla- synergist, fail-fast config.

All new mechanics follow the composition-over-inheritance rule (`AGENTS.md` §1) — they are registered in `Skilling.java` during `onEnable()`, never hardcoded to a skill.

---

## Prerequisites

- Phase 1 complete (trigger field exists)
- Phase 2 complete (namespaced effect keys work — several new mechanics use them)
- `SKILL-REVIEW.md` Part 2 (P2-2 through P2-10, P2-12)

---

## Commit 3.1 — `core:knockback` mechanic (P2-2)

**Purpose:** Applies a directional velocity impulse (knockback) to the damaged entity, or all entities in a radius if `radius > 0`. Backs Shield Bash (shields L25), Heavy Weapons shove, future positional mechanics.

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/KnockbackMechanic.java` | New class implementing `SkillMechanic`. Parameters: `force` (double), `radius` (double, 0 = single target only), `vertical` (double, upward component, default 0.3). If event is `EntityDamageByEntityEvent` and radius is 0, apply velocity to `de.getEntity()`. If radius > 0, iterate `de.getEntity().getNearbyLivingEntities(radius)` and apply to all except the player. If event is `PlayerInteractEvent` (radial shove from player), iterate `player.getNearbyLivingEntities(radius)`. Class-level Javadoc per `AGENTS.md`. |
| `src/main/java/.../Skilling.java` | Register: `mechReg.register("core:knockback", KnockbackMechanic.class, List.of("force", "radius", "vertical"));` |

**Validation:** `./gradlew build && ./gradlew test`

**Pillar compliance:** Pillar II (knockback via vanilla `setVelocity` — a native physics vector), Pillar V (runs inside `EntityDamageByEntityEvent` listener, O(1) for single-target, O(N) bounded by radius but capped by `getNearbyLivingEntities`).

**Test:** `KnockbackMechanicTest` — verify that with `radius=0` and a mock `EntityDamageByEntityEvent`, the target entity's velocity is set. Verify `force=0` returns `false` (no-op).

```
feat(engine): add core:knockback mechanic

KnockbackMechanic applies a directional velocity impulse to the
damaged entity or all living entities in a radius. Supports both
single-target (EntityDamageByEntityEvent) and radial shove
(PlayerInteractEvent) modes. Backs Shield Bash and push mechanics.
```

---

## Commit 3.2 — `core:shield_disable` mechanic (P2-3)

**Purpose:** Sets the vanilla shield raise-lockout cooldown on a target player via `player.setCooldown(Material.SHIELD, ticks)`. Backs Shield Bash (shields L25).

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/ShieldDisableMechanic.java` | New class implementing `SkillMechanic`. Parameter: `ticks` (double). If event is `EntityDamageByEntityEvent` and `de.getEntity() instanceof Player victim`, call `victim.setCooldown(Material.SHIELD, ticks.intValue())`. Return `true` on success, `false` if target is not a player or ticks ≤ 0. Class-level Javadoc. |
| `src/main/java/.../Skilling.java` | Register: `mechReg.register("core:shield_disable", ShieldDisableMechanic.class, List.of("ticks"));` |

**Validation:** `./gradlew build && ./gradlew test`

**Pillar compliance:** Pillar III (enhances the vanilla shield-disable mechanic, doesn't replace it), Pillar V (native API call inside event listener).

**Test:** `ShieldDisableMechanicTest` — verify a mock Player target receives `setCooldown(Material.SHIELD, N)` call. Verify non-player target returns `false`.

```
feat(engine): add core:shield_disable mechanic

ShieldDisableMechanic triggers the vanilla shield raise-lockout
cooldown on a target player via setCooldown(Material.SHIELD, ticks).
This enables Shield Bash to disable enemy shields as a genuine
combat mechanic rather than just reflecting flat thorns damage.
```

---

## Commit 3.3 — `core:offhand_strike` mechanic (P2-4)

**Purpose:** Deals damage equal to the off-hand weapon's base attack damage to the entity the player is looking at. Consumes 1 off-hand item durability. Enables the genuine two-weapon loop for Dual Wield.

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/OffhandStrikeMechanic.java` | New class implementing `SkillMechanic`. Parameters: `multiplier` (double, scales offhand item damage), `reach` (double, max distance, default 4). Requires `PlayerInteractEvent`. Raycast `player.getTargetEntityExact((int) reach)`; if a `LivingEntity` is found, compute base damage from the offhand item's material (using a hardcoded Material→damage map for vanilla weapons), call `target.damage(baseDmg * multiplier, player)`, and consume 1 durability from the offhand item (`itemMeta.setDamage(damage+1)`, respecting `Damageable`/unbreaking). Return `true` on hit, `false` if no target in range. Class-level Javadoc. |
| `src/main/java/.../Skilling.java` | Register: `mechReg.register("core:offhand_strike", OffhandStrikeMechanic.class, List.of("multiplier", "reach"));` |

**Validation:** `./gradlew build && ./gradlew test`

**Pillar compliance:** Pillar III (requires an off-hand weapon — no free attack; consumes durability), Pillar V (raycast + damage call inside `PlayerInteractEvent` listener, O(1)).

**Test:** `OffhandStrikeMechanicTest` — verify that with a mock player holding a sword in offhand and a mock target entity in range, `target.damage()` is called. Verify no target in range returns `false`.

**Design note:** The base-damage lookup per Material should be a simple `static final Map<Material, Double>` initialized with vanilla weapon values (wooden_sword=4, stone_sword=5, iron_sword=6, diamond_sword=7, netherite_sword=8, axe values for sword-tagged axes). This avoids NMS — it's a data lookup, not an API for reading internal damage attributes.

```
feat(engine): add core:offhand_strike mechanic for dual-wield loop

OffhandStrikeMechanic deals damage based on the off-hand weapon's
base attack damage to the entity the player is looking at, consuming
1 offhand durability. This enables a genuine two-weapon loop for
Dual Wield — left-click triggers an offhand strike rather than the
main-hand swing, creating a real dual-wield combat rhythm.
```

---

## Commit 3.4 — `core:set_cooldown` mechanic (P2-9)

**Purpose:** Sets a visual item-stack cooldown (`player.setCooldown(material, ticks)`) for self-disable timing (Shield Bash reuse lockout, instrument performance cooldown, etc.).

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/SetCooldownMechanic.java` | New class implementing `SkillMechanic`. Parameters: `material` (String, e.g. `"minecraft:shield"`), `ticks` (double). Calls `player.setCooldown(Material.matchMaterial(material), ticks.intValue())`. Return `true` on success, `false` if material is null or ticks ≤ 0. Class-level Javadoc. |
| `src/main/java/.../Skilling.java` | Register: `mechReg.register("core:set_cooldown", SetCooldownMechanic.class, List.of("material", "ticks"));` |

**Validation:** `./gradlew build && ./gradlew test`

**Test:** `SetCooldownMechanicTest` — verify `player.setCooldown(Material.SHIELD, N)` is called with `"minecraft:shield"`. Verify invalid material returns `false`.

```
feat(engine): add core:set_cooldown mechanic

SetCooldownMechanic triggers the vanilla item-stack cooldown
animation via player.setCooldown(material, ticks). Used by Shield
Bash to self-disable the shield after a bash, and by Bard
instrument performances to enforce a reuse delay.
```

---

## Commit 3.5 — `core:modify_attack_speed` mechanic (P2-10)

**Purpose:** Temporarily modifies the `ATTACK_SPEED` attribute for a duration. Backs One-Handed "Duelist," Dual Wield rapid strikes. Replaces the fragile numeric `core:modify_attribute` ID 9 usage.

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/mechanic/impl/ModifyAttackSpeedMechanic.java` | New class implementing `SkillMechanic`. Parameters: `multiplier` (double, 1.2 = +20%), `duration` (double, seconds). Resolve `Attribute.ATTACK_SPEED`, compute `added = base * (multiplier - 1)`, add transient `AttributeModifier` (UUID, ADD_NUMBER), schedule removal via `player.getScheduler().runDelayed()`. Modeled on `SpeedBonusMechanic`/`ArmorBonusMechanic`. Class-level Javadoc. |
| `src/main/java/.../Skilling.java` | Register: `mechReg.register("core:modify_attack_speed", ModifyAttackSpeedMechanic.class, List.of("multiplier", "duration"));` |

**Validation:** `./gradlew build && ./gradlew test`

**Test:** `ModifyAttackSpeedMechanicTest` — verify a transient modifier is added to `ATTACK_SPEED` and scheduled for removal. Verify `multiplier=0` or `multiplier=1` returns `false`.

```
feat(engine): add core:modify_attack_speed mechanic

ModifyAttackSpeedMechanic temporarily modifies the ATTACK_SPEED
attribute using a transient modifier, modeled on SpeedBonusMechanic.
This is a clean, purpose-built replacement for core:modify_attribute
with numeric ID 9, which was fragile and uncategorized.
```

---

## Commit 3.6 — `resurrect` trigger (P2-7)

**Purpose:** Fires when a Totem of Undying activates (`EntityResurrectEvent`). Enables L75 totem-synergy passives in Piety.

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/trigger/impl/ResurrectTrigger.java` | New class implementing `SkillTrigger`. Event class: `EntityResurrectEvent`. |
| `src/main/java/.../engine/listener/SkillEventListener.java` | Add `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onResurrect(EntityResurrectEvent event)` that checks `event.getEntity() instanceof Player player` and dispatches. |
| `src/main/java/.../Skilling.java` | Register: `trigReg.register("resurrect", ResurrectTrigger.class);` |

**Validation:** `./gradlew build && ./gradlew test`

**Pillar compliance:** Pillar III (enhances the existing Totem of Undying — the totem must be in the player's hand/inventory and consumed by vanilla mechanics; the trigger just hooks the activation), Pillar V (single event listener, O(1)).

**Test:** No unit test (Bukkit event). Manual verification: hold a totem, take lethal damage, confirm `resurrect` trigger fires.

```
feat(engine): add resurrect trigger for totem-of-undying activation

The resurrect trigger fires on EntityResurrectEvent, enabling
L75 totem-synergy passives in the Piety skill. This hooks the
vanilla totem activation without replacing or duplicating its
behavior — the totem must still be held and is consumed normally.
```

---

## Commit 3.7 — `elytra_glide` trigger (P2-8)

**Purpose:** Fires when a player starts gliding with an elytra (`EntityToggleGlideEvent`). Enables Acrobatics L75 elytra synergy.

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/trigger/impl/ElytraGlideTrigger.java` | New class implementing `SkillTrigger`. Event class: `EntityToggleGlideEvent`. |
| `src/main/java/.../engine/listener/SkillEventListener.java` | Add `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onElytraGlide(EntityToggleGlideEvent event)` that checks `event.getEntity() instanceof Player player && event.isGliding()` and dispatches. |
| `src/main/java/.../Skilling.java` | Register: `trigReg.register("elytra_glide", ElytraGlideTrigger.class);` |

**Validation:** `./gradlew build && ./gradlew test`

**Pillar compliance:** Pillar III (requires an elytra item — no free flight), Pillar V (single event listener, O(1)).

```
feat(engine): add elytra_glide trigger for gliding activation

The elytra_glide trigger fires on EntityToggleGlideEvent when a
player starts gliding, enabling Acrobatics L75 elytra synergy
passives. This hooks the vanilla glide toggle without replacing
the elytra item requirement.
```

---

## Commit 3.8 — Cooldown evaluator support (P2-12)

**Purpose:** Allow the `requirements.cooldown` field to accept evaluator syntax (linear/milestones) in addition to a flat double, enabling inverse-CD sub-scaling per `SKILL-DESIGN-FRAMEWORK.md` §2B.

**Files:**
| File | Change |
|------|--------|
| `skilling-api/.../engine/SkillDefinition.java` | Change `Requirements.cooldown` from `double` to a resolved `ParameterEvaluator` (or keep `double` and add an optional `ParameterEvaluator cooldownEvaluator` field). The decision should minimize breakage — if the parser detects a number, it wraps it in a `ConstantEvaluator`; if it detects a map (evaluator syntax), it resolves it. |
| `src/main/java/.../engine/SkillManager.java` | Parse `requirements.cooldown` as either a scalar (→ `ConstantEvaluator`) or a map (→ resolved evaluator). Fail-fast on malformed evaluator. |
| `src/main/java/.../engine/requirements/RequirementEngine.java` | In `check()`, evaluate the cooldown evaluator against `(skillLevel, unlockLevel)` before comparing against the last-use timestamp. This makes cooldown duration dynamic per level. |
| `web/.../SkillSerializer.java` | Serialize/deserialize cooldown as either a number or an evaluator object (same pattern as all other evaluator params). |

**Validation:** `./gradlew build && ./gradlew test`

**Test:** `RequirementEngineCooldownTest` — verify that a `linear` cooldown evaluator produces a decreasing cooldown as level increases. Verify a scalar cooldown wraps to `ConstantEvaluator` and produces the same behavior as before.

**Breaking change note:** This changes the `Requirements` record signature. All callers and the web serializer must be updated in the same commit. Mark as `feat(requirements)!` if the record change breaks binary compat.

```
feat(requirements)!: support evaluator syntax for ability cooldowns

The requirements.cooldown field now accepts evaluator syntax
(linear/milestones/constant) in addition to a plain double,
enabling inverse-cooldown sub-scaling per SKILL-DESIGN-FRAMEWORK
§2B. A scalar input is wrapped in ConstantEvaluator for backward
compatibility. The Requirements record signature changes.
```

---

## Commit 3.9 — Web GUI: update fallback lists for new mechanics/triggers

**Files:**
| File | Change |
|------|--------|
| `web/frontend/src/components/skills/AbilitiesSection.vue` | Update `FALLBACK_MECHANICS` array to include `core:knockback`, `core:shield_disable`, `core:offhand_strike`, `core:set_cooldown`, `core:modify_attack_speed`, `core:ally_aura`. Update `FALLBACK_PARAM_NAMES` with their parameter lists. |
| `web/frontend/src/components/skills/XpSourcesSection.vue` | Update `FALLBACK_TRIGGERS` to include `resurrect`, `elytra_glide`. |
| `web/frontend/src/components/skills/AbilitiesSection.vue` | Update `TITLE` fallback triggers list (if separate from XpSources). |

**Validation:** `cd web/frontend && npm run build`

**Note:** These fallbacks are only used when the `/api/mechanics` or `/api/triggers` endpoints fail. The API already returns live registry keys, so the frontend will discover the new pieces automatically. The fallback update is for offline resilience and documentation coherence.

```
chore(web): update fallback mechanic and trigger lists for new pieces

Fallback arrays in AbilitiesSection and XpSourcesSection now include
the new mechanics (knockback, shield_disable, offhand_strike,
set_cooldown, modify_attack_speed, ally_aura) and triggers (resurrect,
elytra_glide) added in Phase 3. These are used only when the API
is unavailable; the live registry endpoints already surface them.
```

---

## Phase 3 Completion Checklist

- [ ] `core:knockback` registered, applies velocity impulse
- [ ] `core:shield_disable` registered, sets shield cooldown
- [ ] `core:offhand_strike` registered, deals offhand damage + durability
- [ ] `core:set_cooldown` registered, sets item cooldown
- [ ] `core:modify_attack_speed` registered, transient ATTACK_SPEED modifier
- [ ] `resurrect` trigger registered + listener in SkillEventListener
- [ ] `elytra_glide` trigger registered + listener in SkillEventListener
- [ ] Cooldown evaluator support in RequirementEngine
- [ ] Web fallback lists updated
- [ ] `./gradlew build` passes
- [ ] `./gradlew test` passes (new mechanic tests, cooldown test)
- [ ] `cd web/frontend && npm run build` passes

## Dependencies on Later Phases

- Phase 4 redesigned skills reference all new mechanic keys and trigger keys. The YAMLs cannot load (mechanic not found → skipped) until these registrations exist.
- Phase 5 will document all new pieces in `docs/capabilities.md`.