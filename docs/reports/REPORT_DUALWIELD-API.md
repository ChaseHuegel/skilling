# REPORT: DualWield Plugin API Integration (ranull) for the `dual_wield` skill

**Owning ticket:** [ISSUE-175](../issues/ISSUE-175.md)
**Date:** 2026-08-02
**Type:** Research (feasibility/design study — no production code changes)

---

## 1. Overview

This report evaluates integrating the **DualWield** Bukkit plugin API into Skilling so
the `dual_wield` skill can natively track off-hand attacks and off-hand block breaks
instead of relying only on the current `offhand:weapon` state filter. The core finding is
that **DualWield already double-fires through Skilling's existing `entity_damage` and
`block_break` handlers today**, because its off-hand events are dispatched as *subclasses*
of the base Bukkit events in addition to the base events. An integration therefore needs a
deduplication (suppression) strategy, not just new listeners.

## 2. API surface (verified against the real source)

Source repo: **`AvarionMC/dualwield`** (author **Ranull** originally; forked by AvarionMC
for 1.20.2+ compatibility). Files verified from `master` on 2026-08-02:

- `core/src/main/java/org/avarion/dualwield/event/OffHandAttackEvent.java`
- `core/src/main/java/org/avarion/dualwield/event/OffHandBlockBreakEvent.java`
- `core/src/main/java/org/avarion/dualwield/listener/EntityDamageByEntityListener.java`
- `core/src/main/java/org/avarion/dualwield/listener/BlockBreakListener.java`
- `core/src/main/resources/plugin.yml`

### Events (exact signatures)

```java
// package is org.avarion.dualwield.event — NOT com.ranull.dualwield (ticket correction)
public class OffHandAttackEvent extends EntityDamageByEntityEvent {
    public OffHandAttackEvent(Entity damager, Entity damagee, DamageCause cause, double damage)
}
public class OffHandBlockBreakEvent extends BlockBreakEvent {
    public OffHandBlockBreakEvent(Block block, Player player)
}
```

> **Correction to the ticket's reference:** the constructor signatures in ISSUE-175 match,
> but the package is `org.avarion.dualwield.event`, not `com.ranull.dualwield`. The README's
> Maven snippet still uses `com.ranull:dualwield`, so both spellings appear in the wild.

### Coordinates, target, and version

| Field | Value |
|---|---|
| Group/artifact (README) | `com.ranull:dualwield` |
| Group/artifact (POM) | `org.avarion:dualwield-parent` / `dualwield-core` |
| Repository | GitHub Packages `https://maven.pkg.github.com/AvarionMC/dualwield/` |
| Spigot/Paper API target | builds against `spigot-api 1.20.1-R0.1-SNAPSHOT`, `api-version: 1.13` |
| Latest release | latest `master` (2026); supports up to 1.20.x |

**Runtime detection:** `Bukkit.getPluginManager().getPlugin("DualWield") != null`
(the plugin main is `org.avarion.dualwield.DualWield`; add `DualWield` to
`paper-plugin.yml` `softdepend`).

### Version risk (important)

DualWield is built on **per-version NMS modules** (e.g. `v1_10_R1`…`v1_20_R1`). The fork's
README explicitly states original support ended at 1.20.2. Skilling targets Paper **1.21.8**.
**There is no verified 1.21.x NMS module**, so the plugin may not run on a 1.21 server.
This must be validated before committing to the integration; if DualWield cannot run on
1.21, the integration is moot and the `dual_wield` skill should keep its state-filter
approach (with the double-fire dedup fix below still applied, since DualWield's events are
already observable when the plugin is present).

## 3. How DualWield fires its events (the double-dispatch mechanism)

From the verified listeners:

```java
// EntityDamageByEntityListener — @EventHandler(priority = EventPriority.LOWEST)
// On a dual-wielder attack: swap hands, call OffHandAttackEvent, swap back,
// then copy damage/cancelled onto the BASE EntityDamageByEntityEvent.
```

- The **base** `EntityDamageByEntityEvent` fires first (server-initiated).
- DualWield (LOWEST) creates the `OffHandAttackEvent`, calls `PluginManager.callEvent(...)`,
  then copies `setDamage(...)` and `setCancelled(...)` back onto the base event.
- The base event is **not cancelled by default**; it continues through the event pipeline.
- Because `OffHandAttackEvent extends EntityDamageByEntityEvent`, **Bukkit dispatches the
  subclass to every handler registered on `EntityDamageByEntityEvent`** — including
  Skilling's `onEntityDamage` (MONITOR) — *in addition to* the base event dispatch.

`BlockBreakListener` (LOWEST) does the same for `OffHandBlockBreakEvent` (copies
cancelled / exp-to-drop / drop-items onto the base `BlockBreakEvent`; base not cancelled).

### Consequence for the current skill

Skilling's `onEntityDamage` (`entity_damage`) and `onBlockBreak` (`block_break`) handlers
are registered on the base classes at MONITOR. When DualWield is present, **a single
off-hand attack currently produces TWO dispatches** through `entity_damage` (base + subclass)
and two through `block_break` for off-hand mining. The `dual_wield.yml` skill's
`offhand:weapon` filter is true during **both** dispatches, so the skill is **already
double-granting XP and double-firing abilities** for off-hand actions when DualWield is
installed. This is a latent bug independent of any new integration.

## 4. Recommended design

### 4.1 New trigger keys

Add two built-in triggers (implementations in `engine/trigger/impl`, registered in
`TriggerRegistry`):

| Trigger key | Event class |
|---|---|
| `dualwield:offhand_attack` | `org.avarion.dualwield.event.OffHandAttackEvent` |
| `dualwield:offhand_block_break` | `org.avarion.dualwield.event.OffHandBlockBreakEvent` |

These map cleanly onto the existing `dispatch(player, event, triggerKey)` pipeline; XP
sources and abilities bind to them like any other trigger. The `offhand:weapon` filter
becomes redundant for off-hand sources (the event *is* the off-hand action) but remains
valid for main-hand + offhand-holding sources.

### 4.2 Deduplication (required)

Because the subclass events also arrive at the base handlers, the integration must
**suppress the base dispatch** when an off-hand subclass was observed for the same action:

- The subclass is dispatched synchronously **inside** the base event's LOWEST phase, and
  Skilling's base handlers run at MONITOR (after LOWEST). So a marker set during the
  subclass dispatch is still present when the base event reaches MONITOR.
- **Approach:** in the `dualwield:offhand_attack` / `dualwield:offhand_block_break`
  dispatch path, record a per-player transient flag (main-thread only; a
  `ConcurrentHashMap<UUID, Boolean>` or an event-scoped set). The base
  `entity_damage` / `block_break` handlers check the flag, clear it, and skip if set.
  Events are dispatched sequentially on the main thread, so a single consumed flag is
  race-free.
- **Also apply the fix when DualWield is present but no new triggers are used:** the same
  suppression stops the current double-grant. Without DualWield installed the flag is never
  set and behavior is unchanged.

### 4.3 Where the listeners register

Mirror the existing `IntegrationManager` soft-dependency pattern (`PlaceholderAPI`/`Vault`):
- `IntegrationManager.initialize()` checks `getPlugin("DualWield") != null` and, if present,
  registers a `DualWieldHook` listener with the plugin manager.
- The hook listener observes `OffHandAttackEvent`/`OffHandBlockBreakEvent` at MONITOR
  (`ignoreCancelled = true`), sets the suppression flag, and calls the existing
  `SkillEventListener` dispatch with the new trigger keys. **Observe only — never cancel.**

### 4.4 compileOnly vs reflection

- **compileOnly + `softdepend`:** consistent with the existing PAPI/Vault pattern, but
  requires the DualWield artifact in the build (GitHub Packages repo, or a checked-in jar
  in `libs/`) and only works on a version with a published API jar.
- **Reflection (recommended first):** register a listener whose handler signature is built
  reflectively from the class name (`org.avarion.dualwield.event.OffHandAttackEvent`),
  resolved only when the plugin is present. Keeps the build dependency-free and survives
  version drift; falls back gracefully (no hook) when the classes are absent.
- `skilling-api` must remain dependency-free either way (the hook lives in `src/`, not the
  API module).

### 4.5 Priority / cancellation semantics

DualWield fires at LOWEST; Skilling should observe at MONITOR. The hook must **never
cancel** and must not mutate damage (DualWield already copies subclass → base). This
interacts with ISSUE-121's damage-cancel priorities: `entity_damage_taken` (LOWEST) is for
the *defender* and is unaffected by off-hand *attack* events.

### 4.6 Thread safety

The suppression flag is main-thread-only; guard with `ConcurrentHashMap` and document that
the consume-once guarantee (ISSUE-116) is per-activation. No async work in the hook.

## 5. `dual_wield.yml` skill redesign

Current sources/abilities and the proposed changes:

| Current | Issue | Proposed |
|---|---|---|
| `entity_damage` + `offhand:weapon` (XP + `dual_strike`, `whirlwind`, `rapid_assault`, `storm_blade`) | double-fires when DualWield present | keep `entity_damage` for main-hand; off-hand attacks move to `dualwield:offhand_attack` with the suppression fix |
| `entity_kill` + `offhand:weapon` | same double risk on the kill event | add `dualwield:offhand_attack`-scoped kill or keep `entity_kill` + `offhand:weapon` (a kill is a single event; the attack that killed already double-fired — suppression fixes it) |
| `player_interact` `offhand_strike` ability | unrelated to DualWield events | unchanged |
| `entity_damage_taken` `dual_parry` | unaffected | unchanged |
| (none) block break | off-hand mining earns nothing today | add `dualwield:offhand_block_break` XP source + a mining-friendly ability |

The `offhand:weapon` state filter remains useful for main-hand triggers while holding an
off-hand weapon; it is redundant for the dedicated off-hand triggers.

## 6. Risk / performance

- **Performance:** the hook adds one MONITOR dispatch per off-hand attack/break. Both
  events are already being dispatched through the base handlers today, so the marginal cost
  is one extra trigger-key dispatch + a flag set/clear — negligible relative to the existing
  per-event skill loop.
- **1.21 compatibility is the gating risk** (§2). Validate DualWield runs on 1.21 before
  building the skill content on it.
- **Dedup correctness** depends on the synchronous-dispatch ordering guarantee (subclass
  within base's LOWEST phase). This holds for DualWield's current implementation; the hook
  should document that assumption and fail closed (never suppress when the flag is
  ambiguous).

## 7. Recommendation

1. **Implement the suppression fix regardless** (fixes the existing double-grant whenever
   DualWield is present) — low risk, correct either way.
2. **Gate the full integration on a 1.21 smoke test of DualWield.** If it runs: add the two
   trigger keys, the reflection-based hook in `IntegrationManager`, the `softdepend` entry,
   and rework `dual_wield.yml` to use `dualwield:offhand_attack` / `dualwield:offhand_block_break`.
3. If DualWield does not run on 1.21, keep the current state-filter design and only land the
   suppression fix.

## 8. Follow-up implementation tickets

- **feat(engine): register `dualwield:offhand_attack` / `dualwield:offhand_block_break` triggers**
  (two `SkillTrigger` implementations + `TriggerRegistry` registration).
- **feat(integration): DualWield hook + base-dispatch suppression** (reflection-based
  listener in `IntegrationManager`, per-player transient flag consumed by the base
  `entity_damage`/`block_break` handlers; `DualWield` in `softdepend`).
- **feat(skills): rework `dual_wield.yml`** to bind off-hand XP/abilities to the new triggers
  and add an off-hand mining source.
- **test(engine):** suppression unit test (subclass dispatch sets flag; base dispatch skips;
  flag clears) using a mocked `OffHandAttackEvent`/`OffHandBlockBreakEvent`.

## References

- Real event source: <https://github.com/AvarionMC/dualwield/blob/master/core/src/main/java/org/avarion/dualwield/event/OffHandAttackEvent.java>
- Real event source: <https://github.com/AvarionMC/dualwield/blob/master/core/src/main/java/org/avarion/dualwield/event/OffHandBlockBreakEvent.java>
- Firing listeners: `EntityDamageByEntityListener.java`, `BlockBreakListener.java` (same repo)
- README: <https://github.com/AvarionMC/dualwield/blob/master/README.md>
