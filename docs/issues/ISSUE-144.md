# ISSUE-144: Fix the `%skilling_total_levels%` placeholder (always returns "0")

**Status:** Open
**Type:** Bug
**Severity:** High (documented placeholder is dead)

---

## Context & User Story

- **Goal:** As a server owner, I want `%skilling_total_levels%` in chat/scoreboard/PlaceholderAPI to show the sum of all the player's skill levels.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Fix the action parsing in `PlaceholderAPIHook.onRequest` so `total_levels` is recognized (currently `params.split("_", 2)` turns it into `["total", "levels"]` and the check can never match)
- [ ] Implement `total_levels` to sum the player's level across all loaded skills
- [ ] Keep the single-skill placeholders (e.g. `%skilling_mining%`) working
- [ ] Add a unit test covering: `total_levels` placeholder returns the summed level, single-skill placeholder unchanged

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/integration/PlaceholderAPIHook.java:52,58-67`
- **Dependencies:** `PlayerProfile`/`SkillManager` for level computation (`getLevelForXp`).
- **Constraints:** Keep the expansion API stable; only fix the parsing/implementation.

### Root Cause

`onRequest` does `params.split("_", 2)`. For `"total_levels"` that yields `["total", "levels"]`, so `action` is `"total"` and never matches `"total_levels"`. Execution falls through to `getSkill("levels")` → null → `"0"`. The documented total-levels placeholder is dead.

### Proposed Fix

Match the full placeholder string before splitting, or split on the first `_` only after checking for `total_levels`. Implement the sum over `skillManager.getSkills()` using the profile's XP per skill.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new regression test
- [ ] Unit test: `%skilling_total_levels%` returns the correct summed value
- [ ] Unit test: `%skilling_<skill>%` single-skill expansion is unchanged
