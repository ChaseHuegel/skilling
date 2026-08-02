# ISSUE-176: Add "bury bones" as a `player_interact` XP source and ability for the `piety` skill

**Status:** Open
**Type:** Improvement
**Severity:** Medium (new content feature + a small engine extension to support block-target filters on `player_interact`)

---

## Context & User Story

- **Goal:** As a server admin, I want players to be able to "bury" bones by right-clicking dirt, grass block, or coarse dirt with a bone in hand — consuming 1 bone, playing a minor vanilla+-feeling particle effect (similar to bonemeal) — and grant `piety` XP.
- **Agent Role:** You are an expert backend engineer executing this task (content + the engine support it requires).

## Implementation Requirements

- [ ] Add a `bury_bones` (or `burial`) ability to `src/main/resources/skills/piety.yml`: triggers from `player_interact`, fires only on right-click on dirt / grass block / coarse dirt while holding a bone, **costs 1 bone** (consumed on activation), and plays a bonemeal-like particle/sound effect at the clicked block
- [ ] Add a `piety` XP source mirroring the same trigger: `player_interact` on dirt / grass block / coarse dirt while holding a bone, granting XP
- [ ] Support block-target filtering on the `player_interact` trigger: extend `resolveEventMaterial` (`SkillEventListener.java:662-693`) to resolve `PlayerInteractEvent.getClickedBlock().getType()`, guarded to right-click-on-block (`event.getAction() == RIGHT_CLICK_BLOCK`), so `filters: [ { target: ... } ]` can match the buried block
- [ ] Provide an executable action for the ability so the Check-Execute-Consume path runs (an ability with zero mechanics never reaches `consume`, so the bone cost would not be deducted): either a small new `core:` mechanic that spawns a configured particle set at the clicked block, or reuse the feedback system with the block as the particle/sound target
- [ ] Add tests covering: the piety YAML parses (XP source + ability), the `player_interact` block-target filter resolves the clicked block (and does not match left-clicks/air), the bone cost is consumed exactly once per activation, and the particle mechanic/feedback fires at the block

## Technical Specifications & Context

- **Target Files:**
  - `src/main/resources/skills/piety.yml` (new XP source + ability; currently only `breed_animals` and `resurrect` XP sources)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:662-693` (`resolveEventMaterial` — add `PlayerInteractEvent` handling)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:520-576` (`matchFilter` — already supports `target` and `tool`, but the target resolution above is the missing piece)
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/FanfareDispatcher.java:46-71` (particle dispatch already accepts a `Location target`; `fireAbilities` currently passes `null` at `SkillEventListener.java:509`)
  - Optionally a new `core:` mechanic (e.g. `core:block_particles`) registered in `Skilling.java`
- **Dependencies / Existing Capabilities:**
  - Item cost: `RequirementEngine` already supports `items: [ { action: "cost", tag: ..., amount: 1 } ]`, deducted on `consume` (`RequirementEngine.java:85-92,130-134`).
  - Tool filter: `matchFilter` matches `tool` against `player.getInventory().getItemInMainHand()` (`SkillEventListener.java:558-559`) — a bone in the main hand.
  - Block target: NOT yet supported on `player_interact` — `resolveEventMaterial` has no `PlayerInteractEvent` branch, so a `target` filter on this trigger currently fails (`matchFilter` returns false when the material is null, line 534).
  - Ability feedback supports particles/sounds with a `target: "target"` mode but is currently invoked with a null target (player location).
- **Constraints:** Pure content for the skill (YAML) plus the minimal engine support listed above. No hardcoded skills — the mechanic stays generic and config-driven. The action guard must be `RIGHT_CLICK_BLOCK` so left-click mining of dirt with a bone does not trigger the bury.

### Design Notes

- Block set: dirt, grass block, coarse dirt (the user-named trio). The vanilla `#minecraft:dirt` tag covers these plus podzol/mycelium/rooted dirt — either the explicit list or the tag is acceptable; prefer the explicit trio to match the requested scope, or the tag for a "vanilla+" feel.
- Effect: `ITEM_BONE_MEAL_USE` sound + a bonemeal-like particle (e.g. `HAPPY_VILLAGER`) at the clicked block, configured via ability feedback or the new mechanic.
- XP reward: piety is currently among the slowest skills (~3,600 h to 100 per `REPORT_XP-CURVE.md`); a high-frequency bury source should use a modest constant (e.g. 5–8 XP per burial) and would be a meaningful balance improvement — calibrate per the report's reward framework.
- Design consideration to confirm: as specified, the **XP source** fires on the interaction while holding a bone regardless of whether the (level-gated) ability fires and consumes the bone. If XP should only accrue on an actual burial, gate the source additionally (e.g. require the ability, or accept the interaction-only source as written).
- Interplay: ISSUE-116 changes ability consume to once-per-ability; this ability has a single mechanic, so today's per-mechanic consume is safe, but verify after ISSUE-116 lands that the bone is still consumed once.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new tests
- [ ] `SkillYamlValidationTest` passes with the updated `piety.yml` (new XP source + ability parse)
- [ ] Unit test: `player_interact` on dirt/grass block/coarse dirt resolves the clicked block as the target material; left-click and right-click-air do not match
- [ ] Unit test: activating the ability consumes exactly 1 bone
- [ ] Unit test: the particle/sound effect targets the clicked block location
- [ ] Manual smoke: right-click dirt with a bone → bone consumed, bonemeal-like particles at the block, `piety` XP granted
