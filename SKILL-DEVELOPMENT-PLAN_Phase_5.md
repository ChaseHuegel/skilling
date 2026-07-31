# SKILL-DEVELOPMENT-PLAN — Phase 5: Documentation Sweep

**Review references:** §3.3 (Framework/docs/capabilities.md Corrections Required) of `SKILL-REVIEW.md`
**Priority:** Final parity pass — all docs must reflect the shipped state after Phases 1–4
**Estimated commits:** 4

---

## Objective

Synchronize all user-facing documentation in `docs/`, the `template-skill.yml` template, and the `capabilities.md` catalog with the engine changes from Phases 1–4. Per `AGENTS.md` §Documentation: docs are user-facing and must use clear language free of implementation jargon.

All corrections from `SKILL-REVIEW.md` §3.3 (Framework/docs/capabilities.md Corrections Required) must be applied.

---

## Prerequisites

- Phases 1–4 complete (all engine pieces exist and all skill YAMLs are remediated)
- `SKILL-REVIEW.md` §3.3 (table of required corrections)
- `DESIGN.md` §8 (documentation structure requirements)

---

## Commit 5.1 — Update `docs/capabilities.md` (mechanics, triggers, evaluators catalog)

This is the addon-developer reference catalog. Per `DESIGN.md` §8, it must list every built-in mechanic, trigger, and evaluator with parameters and YAML usage.

**Files:**
| File | Change |
|------|--------|
| `docs/capabilities.md` | **Mechanics section — add new entries:** `core:knockback` (params: force, radius, vertical; event: EntityDamageByEntityEvent/PlayerInteractEvent), `core:shield_disable` (params: ticks; event: EntityDamageByEntityEvent), `core:offhand_strike` (params: multiplier, reach; event: PlayerInteractEvent), `core:ally_aura` (params: effect, radius, duration, amplifier; event: varies via trigger), `core:set_cooldown` (params: material, ticks; event: any), `core:modify_attack_speed` (params: multiplier, duration; event: any). **Triggers section — add new entries:** `resurrect` (EntityResurrectEvent), `elytra_glide` (EntityToggleGlideEvent). **Evaluators section — no change** (no new evaluator types added). **State Filters section — add:** `equipped` (light/medium/heavy/none). **Fix existing entries per §3.3:** `core:speed_bonus` — correct documented event (remove wrong `PlayerToggleSprintEvent` claim; document that it fires on whatever trigger the ability declares). `core:haste_effect` — correct documented event (remove `BlockBreakEvent` as the only event; document it fires on the ability's trigger). `core:field_aura` / `core:aoe_effect` / `core:xp_bonus` — replace "Varies (triggered by ability activation)" with "Fires on the ability's declared trigger event" (now gated by the new `trigger` field). `core:thorns_damage` — fix param name to match `Skilling.java` registration (`damage`, not `percentage`). `core:block_damage` vs `core:cancel_damage` — document the semantic difference (both negate damage via chance; `block_damage` is "block" flavor for armor/shield skills, `cancel_damage` is "dodge/evade" flavor). **Effect/attribute param docs:** document that `effect` and `attribute` parameters now accept namespaced keys (`minecraft:regeneration`) and that legacy numeric IDs are deprecated. |

**Validation:** Review the rendered markdown for completeness. Cross-check every mechanic key in `Skilling.java` `registerBuiltins()` against a `docs/capabilities.md` entry — no orphans in either direction.

```
docs(capabilities): update catalog for new mechanics, triggers, and corrections

Add entries for core:knockback, core:shield_disable,
core:offhand_strike, core:ally_aura, core:set_cooldown,
core:modify_attack_speed, resurrect trigger, elytra_glide trigger,
and equipped state filter. Fix documented events for speed_bonus,
haste_effect, field_aura, aoe_effect, xp_bonus (now gated by
ability trigger field). Fix thorns_damage param name. Document
block_damage vs cancel_damage semantic difference. Document
namespaced effect/attribute key migration with legacy deprecation.
```

---

## Commit 5.2 — Update `docs/creating-skills.md` (YAML schema reference)

This is the server-owner/designer guide. Per `DESIGN.md` §8, it must contain the full YAML schema with annotated examples.

**Files:**
| File | Change |
|------|--------|
| `docs/creating-skills.md` | 1. **Abilities section:** Add `trigger` as a **required** field in the abilities table (after `unlock_level`). Document: "Each ability must declare exactly one trigger key that determines which event dispatches it. See the Triggers table for valid keys." 2. **requirements.cooldown:** Update the table to document that `cooldown` accepts evaluator syntax (linear/milestones/constant) in addition to a double, enabling inverse-CD sub-scaling. Add a commented example. 3. **Filters section:** Add a note clarifying: "`target:` on `entity_damage` matches the **damager projectile material** only (e.g., arrows, tridents). For held-weapon detection on melee `entity_damage`, use `tool:` instead." 4. **State filters table:** Add `equipped:light`, `equipped:medium`, `equipped:heavy`, `equipped:none` entries. 5. **Effect/attribute params:** Add a note that namespaced keys (`minecraft:poison`) are preferred; numeric IDs are deprecated. 6. **Annotated example:** Update the full annotated example to include `trigger` fields on all abilities and namespaced effect keys. |

**Validation:** Cross-check all fields against `docs/capabilities.md` and `template-skill.yml`.

```
docs(creating-skills): add trigger field, equipped filter, cooldown evaluator, target/tool note

Update the YAML schema reference:
- trigger is now a required field on abilities (binds to one event)
- requirements.cooldown accepts evaluator syntax for sub-scaling
- target on entity_damage matches projectile material only; use tool
  for held-weapon detection
- equipped state filter (light/medium/heavy/none) documented
- namespaced effect/attribute keys preferred over legacy numeric IDs
- annotated example updated with trigger fields and namespaced effects
```

---

## Commit 5.3 — Update `template-skill.yml` (annotated template)

**Files:**
| File | Change |
|------|--------|
| `template-skill.yml` | 1. Add `trigger: "block_break"` (commented with explanation) to the passive ability example. 2. Add `trigger: "block_break"` to the active ability example. 3. Replace legacy numeric effect IDs in any example with namespaced keys. 4. Add `equipped:` filter example in a commented block. 5. Add a commented example of evaluator-syntax cooldown (`cooldown: { linear: { base: 5.0, step: -0.02, max: 1.0 } }`). 6. Ensure every key has a commented documentation line per `AGENTS.md` §YAML Template Documentation. |

**Validation:** Verify the template loads correctly if placed in the skills directory (no `IllegalArgumentException`).

```
docs(template): update template-skill.yml with trigger field and namespaced keys

The template now includes trigger fields on ability examples,
namespaced effect keys instead of legacy numeric IDs, a commented
equipped filter example, and a commented evaluator-syntax cooldown
example. All keys have inline documentation per AGENTS.md.
```

---

## Commit 5.4 — Update remaining docs (`api-integration.md`, `configuration.md`, `getting-started.md`) + `docs/capabilities.md` block_damage/cancel_damage merge decision

**Files:**
| File | Change |
|------|--------|
| `docs/api-integration.md` | Add code samples for registering the new mechanic/trigger/evaluator types introduced in Phases 1–3. Document the `trigger` field as a required part of the ability schema for addon developers. Add a section on registering custom `StateFilter` implementations (referencing the new `state:equipped` pattern). Document the namespaced effect/attribute key format for addon mechanics that use `PotionEffectResolver`. |
| `docs/configuration.md` | No structural changes expected (config.yml keys unchanged). Verify the `tags.yml` section mentions that custom tags can cross-reference vanilla tags and other custom tags. If any new custom tags were added to `tags.yml` during Phase 4 (e.g., instrument tags for Bard), document them here. |
| `docs/getting-started.md` | No structural changes expected — this is installation/first-run guidance. Verify no references to removed XP sources or deprecated mechanic behaviors exist. |
| `docs/capabilities.md` (revisit) | Final decision on `block_damage` vs `cancel_damage` (§3.3 of the review): either document their semantic difference clearly (both are `BaseDamageCancelMechanic` subclasses with the same `chance` param; `block_damage` is the "shield/armor block" flavor, `cancel_damage` is the "dodge/evade" flavor) or merge them into one mechanic with a `flavor` parameter. **Recommended:** keep both but document the distinction — merging would break existing skill YAMLs. |

**Validation:** Review all rendered markdown. Cross-link between `api-integration.md` and `capabilities.md`.

```
docs: synchronize api-integration, configuration, getting-started with engine changes

Update api-integration.md with code samples for registering new
mechanics/triggers/state filters and the trigger field requirement.
Document the block_damage vs cancel_damage semantic distinction
(both negate damage by chance; block_damage = shield/armor flavor,
cancel_damage = dodge/evade flavor — kept separate for clarity).
Verify configuration.md and getting-started.md have no stale
references.
```

---

## Phase 5 Completion Checklist

- [ ] `docs/capabilities.md` lists every registered mechanic, trigger, evaluator, and state filter — no orphans
- [ ] `docs/capabilities.md` documented events corrected for speed_bonus, haste_effect, field_aura, aoe_effect, xp_bonus
- [ ] `docs/capabilities.md` thorns_damage param name fixed
- [ ] `docs/capabilities.md` block_damage vs cancel_damage distinction documented
- [ ] `docs/capabilities.md` namespaced key migration documented
- [ ] `docs/creating-skills.md` trigger field documented as required
- [ ] `docs/creating-skills.md` target vs tool filter note added
- [ ] `docs/creating-skills.md` equipped state filter documented
- [ ] `docs/creating-skills.md` cooldown evaluator syntax documented
- [ ] `docs/creating-skills.md` annotated example updated
- [ ] `template-skill.yml` includes trigger fields, namespaced effects, equipped filter example
- [ ] `docs/api-integration.md` has code samples for new pieces
- [ ] All docs cross-checked against `Skilling.java` registrations and `src/main/resources/skills/` YAMLs
- [ ] No stale references to removed/deprecated behaviors

---

## Final Cross-Phase Verification (post-Phase 5)

After all 5 phases are complete, run the full validation suite:

```bash
./gradlew build && ./gradlew test
cd web/frontend && npm run build && npm run e2e
```

All must pass. Additionally, manually verify:
1. Every ability in every bundled skill has a `trigger` field (Phase 4 validation test covers this).
2. No `core:field_aura` usage remains in Bard/Piety (replaced by `core:ally_aura`).
3. No unguarded `player_interact` XP sources remain.
4. light_weapons and heavy_weapons grant XP and fire abilities on melee hits.
5. Armor skills only grant XP/abilities when wearing the correct armor type.