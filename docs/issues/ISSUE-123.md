# ISSUE-123: Wire the `level_up` trigger to `SkillingLevelUpEvent` instead of vanilla level changes

**Status:** Open
**Type:** Bug
**Severity:** High (documented trigger semantics are unreachable)

---

## Context & User Story

- **Goal:** As a skill author, I want `trigger: level_up` to fire when a **Skilling** skill levels up, so level-up abilities and XP sources work as documented.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Dispatch Skilling's own `SkillingLevelUpEvent` through the trigger pipeline with key `level_up`
- [ ] Stop (or make clearly separate) dispatching `level_up` from vanilla `PlayerLevelChangeEvent`
- [ ] Ensure `LevelUpTrigger` (declared on `SkillingLevelUpEvent`) matches the actually-dispatched event
- [ ] Add a unit test verifying a YAML `trigger: level_up` ability fires on a Skilling level-up

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:290-293` (mis-wired dispatch) and `:403-406` (`SkillingLevelUpEvent` fired via `Bukkit.callEvent` only)
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/LevelUpTrigger.java:16` (declares `SkillingLevelUpEvent.class`)
- **Dependencies:** `io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent`.
- **Constraints:** If vanilla-level triggers are a desired feature, add a separate key (e.g. `vanilla_level_up`); do not overload `level_up`.

### Root Cause

`SkillEventListener` dispatches key `level_up` for `PlayerLevelChangeEvent` (vanilla Minecraft levels), but `LevelUpTrigger` declares `SkillingLevelUpEvent`. The `SkillingLevelUpEvent` fired on skill level-ups is never routed through `dispatch(...)`, so the intended `level_up` semantics are unreachable.

### Proposed Fix

When `SkillingLevelUpEvent` is fired (after `profile.addXp` crosses a level threshold, `SkillEventListener.java:403-406`), also route it through `dispatch(player, event, "level_up")`. Remove the vanilla `PlayerLevelChangeEvent` binding for `level_up` unless a separate key is introduced.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: leveling a Skilling skill triggers `level_up` abilities/XP sources
- [ ] Unit test: vanilla Minecraft level changes no longer fire `level_up`
- [ ] `docs/users/capabilities.md` trigger list reflects the corrected behavior
