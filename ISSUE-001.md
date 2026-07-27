# ISSUE-001: Refactor Duplicate `showXpBossBar` and `broadcastLevelUp` Logic

## Description

The `showXpBossBar()` and `broadcastLevelUp()` methods are identically
implemented in two files — `SkillEventListener.java` and `SkillsCommand.java`.
Any bug fix or enhancement to bossbar display or level-up fanfare must be
applied to both copies. This has already caused drift and regression during
development.

## Scope

- `SkillEventListener.java` — lines 195–245 (showXpBossBar), lines 428–526 (broadcastLevelUp + helpers)
- `SkillsCommand.java` — lines 273–323 (showXpBossBar), lines 345–439 (broadcastLevelUp + helpers)
- Shared helpers: `randomBrightColor()`, `spawnFirework()`, `resolveBarColor()`, `mmColorName()`, `isMajorLevelUp()`

## Proposed Solution

Extract the duplicated methods into a shared utility class, e.g.:
`io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher`

### New class responsibilities

| Method | Source |
|--------|--------|
| `showXpBossBar(Player, SkillDef, Profile, BossBarPool, Skilling)` | merged |
| `broadcastLevelUp(Player, SkillDef, int, Skilling)` | merged |
| `isMajorLevelUp(SkillDef, int)` | merged |
| `randomBrightColor()` | merged |
| `spawnFirework(Location, Color, Type, int)` | merged |
| `resolveBarColor(String)` → TextColor | merged |
| `mmColorName(String)` → MiniMessage color name | merged |

### Steps

1. Create `LevelUpDispatcher` class with all static methods migrated from both files.
2. Replace inline implementations in `SkillEventListener` with delegation calls.
3. Replace inline implementations in `SkillsCommand` with delegation calls.
4. Remove now-unused private methods from both source files.
5. Verify no remaining duplicate signatures.
6. Build, test, commit.

## Risk

Low. Pure refactor — no behavior changes. All existing call sites remain
unchanged; only the implementation location moves.
