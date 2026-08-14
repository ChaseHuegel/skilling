# ISSUE-293: `skilling.use` permission used by player commands is undeclared, making `/skills` ops-only

## Context & User Story
- **Goal:** As a player, I want `/skills` to open my skill overview. `SkillsCommand` attaches `skilling.use` to the bare `/skills`, `/skills help`, and `/skills log` commands, but `paper-plugin.yml` declares only `skilling.admin`. Bukkit resolves undeclared permissions to `false` for non-OP players, so the primary player entry point is effectively admin-only.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Declare `skilling.use` in `paper-plugin.yml` with `default: true` and a description.
- [ ] Keep `skilling.admin` at `default: op`.
- [ ] Verify no other permission nodes used in `SkillsCommand` are undeclared.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/paper-plugin.yml:13-16` (permissions block), `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java:75,95,122,130` (`.permission("skilling.use")`), `:163,180,193,206,213` (`.permission("skilling.admin")`).
- **Dependencies:** None.
- **Constraints:** The documented behavior is that a bare `/skills` opens the player overview for all players.

## Verification & Definition of Done
- [ ] `paper-plugin.yml` lists both permissions with correct defaults.
- [ ] `./gradlew build` passes.
- [ ] Edge case handled: `/skills help` and `/skills log` remain usable by non-OP players.
