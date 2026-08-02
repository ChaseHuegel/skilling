# ISSUE-175: Research integrating the DualWield plugin API (ranull) for the `dual_wield` skill

**Status:** Open
**Type:** Research
**Severity:** Medium (feasibility/design study; informs a potential engine + skill integration — no production code changes)

---

## Context & User Story

- **Goal:** As a content designer, I want a data-backed evaluation of integrating the **DualWield plugin API (ranull)** into Skilling so the `dual_wield` skill can natively track off-hand attacks and off-hand block breaks, rather than relying solely on the current `offhand:weapon` state filter.
- **Agent Role:** You are an expert plugin-integration analyst executing this task (research and writing; no production code changes).

## Implementation Requirements

- [x] Document the DualWield API surface relevant to Skilling: the `OffHandAttackEvent` and `OffHandBlockBreakEvent` signatures, their package/coordinates, the plugin's Paper API target and latest version, and how to detect its presence at runtime
- [x] Map both events to Skilling trigger semantics: propose trigger keys (e.g. `dualwield:offhand_attack`, `dualwield:offhand_block_break`) and how `dual_wield` XP sources/abilities would consume them
- [x] Analyze the **subclass-dispatch interaction**: `OffHandAttackEvent extends EntityDamageByEntityEvent` and `OffHandBlockBreakEvent extends BlockBreakEvent`, so Bukkit already routes them into Skilling's existing `entity_damage`/`block_break` handlers — determine whether the same off-hand action fires a base event too (double-grant risk) and propose a suppression/scoping strategy
- [x] Evaluate the integration mechanism: `softdepend` in `paper-plugin.yml` + `compileOnly` vs. pure reflection; where the listeners are registered (`IntegrationManager` pattern); handler priority and cancellation semantics (observe, do not cancel); thread-safety
- [x] Assess the current `dual_wield` skill design (`offhand:weapon` state filter, `entity_damage`/`entity_kill`/`player_interact`/`entity_damage_taken` triggers) and how the DualWield events would improve or replace it
- [x] Assess risk/performance (events fire on every off-hand attack/block break; interaction with ISSUE-121 damage-cancel priorities and ISSUE-116 per-ability consume)
- [x] Recommend an approach and list concrete follow-up implementation tickets

## Technical Specifications & Context

- **Target Files (research inputs only):**
  - `src/main/resources/skills/dual_wield.yml` (the skill to benefit)
  - `src/main/java/io/github/chasehuegel/skilling/engine/integration/IntegrationManager.java` (existing soft-dependency hook pattern: PAPI/Vault)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (dispatch cascade; existing `entity_damage`/`block_break` handlers that will also receive the subclass events)
  - `src/main/resources/paper-plugin.yml` (add `DualWield` to `softdepend`)
- **Dependencies / References (verified from the DualWield repo):**
  - `com.ranull.dualwield.event.OffHandAttackEvent extends EntityDamageByEntityEvent` — constructor `(Entity damager, Entity damagee, DamageCause cause, double damage)`
  - `com.ranull.dualwield.event.OffHandBlockBreakEvent extends BlockBreakEvent` — constructor `(Block block, Player player)`
  - Maven coordinates/repo and latest release of DualWield must be confirmed during research.
- **Constraints:** Skilling must keep zero hard runtime dependencies — the plugin may add DualWield as `softdepend` and `compileOnly` (never shaded), and `skilling-api` must stay dependency-free. No production code changes in this ticket; findings land in the report and follow-up tickets.

### Key Open Questions to Resolve

1. Does DualWield fire its `OffHandAttackEvent` **in addition to** a vanilla `EntityDamageByEntityEvent`, or does it cancel the base event and fire only the subclass? If both fire, the `dual_wield` skill could currently be double-counting once the integration observes the offhand events.
2. What is the correct event priority for Skilling listeners relative to DualWield's own firing (does DualWield fire at a specific priority/order)?
3. Does `OffHandBlockBreakEvent` carry the same drop/filter context (tool, enchantments) as the base `BlockBreakEvent` so XP filters and mechanics behave identically?
4. Should the integration expose new trigger keys, or should off-hand events be folded into the existing `entity_damage`/`block_break` triggers (with the `offhand:weapon` filter becoming redundant)?

## Verification & Definition of Done

- [x] Report delivered at `docs/reports/REPORT_DUALWIELD-API.md` documenting all Implementation Requirements above
- [x] Report cross-references the real DualWield event source (signatures/constructors verified, not guessed)
- [x] Report includes the Double-grant analysis with a concrete recommendation (suppress vs. scope vs. separate keys)
- [x] Report includes a concrete recommended approach and a list of follow-up implementation tickets
- [x] Report renders as clean Markdown; no production code changed by this ticket
