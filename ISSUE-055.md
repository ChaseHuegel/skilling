# Test coverage gaps: 25 mechanics untested, 19 triggers untested, requirement engine lifecycle uncovered, no serialization tests, no `BossBarPool`/`FanfareDispatcher`/`LevelUpDispatcher` tests

## Issue

The project has significant test coverage gaps across the entire engine. While `ParameterEvaluator` implementations, `TagResolver`, and `SkillManager` have unit tests, the following areas have zero coverage:

- **Mechanics (25):** All `SkillMechanic` implementations untested
- **Triggers (19):** All `SkillTrigger` implementations untested
- **RequirementEngine:** Check/Execute/Consume lifecycle untested
- **Serialization:** No tests for YAML parsing, skill definition serialization/deserialization
- **Feedback systems:** `BossBarPool`, `FanfareDispatcher`, `LevelUpDispatcher` untested

**ISSUES.md reference:** Line 311

## Root Cause

Testing focus has been on the data layer (evaluators, tag resolution, skill parsing) while the execution layer (mechanics, triggers, requirements, feedback) remains uncovered.

## Affected Files

| Area | Path | Test File |
|------|------|-----------|
| All 25 mechanics | `engine/mechanic/impl/*.java` | None |
| All 19 triggers | `engine/trigger/impl/*.java` | None |
| RequirementEngine | `engine/requirements/RequirementEngine.java` | None |
| BossBarPool | `engine/feedback/BossBarPool.java` | None |
| FanfareDispatcher | `engine/feedback/FanfareDispatcher.java` | None |
| LevelUpDispatcher | `engine/feedback/LevelUpDispatcher.java` | None |

## Development Plan

### Step 1: Prioritize test targets

| Priority | Module | Reason |
|----------|--------|--------|
| P0 | `RequirementEngine` | Core gameplay gate — check/consume lifecycle bugs directly affect players |
| P0 | All 25 mechanics | Each mechanic has distinct logic that must be verified |
| P1 | `LevelUpDispatcher` | Handles XP bossbar and level-up broadcasts |
| P1 | `FanfareDispatcher` | Sound/title/firework effects |
| P2 | All 19 triggers | Mostly event class mappings — simple to test |
| P2 | `BossBarPool` | Pool lifecycle and TTL management |

### Step 2: Test framework approach

- Use JUnit 5 + Mockito for Bukkit-dependent tests
- Create a `BukkitMock` utility class for common mock setups (player, inventory, events)
- Mechanics: test `execute()` with mocked `Player`, `Event`, and params map
- Triggers: test `getEventClass()` returns correct class
- RequirementEngine: test `check()`, `consume()`, and the full lifecycle

### Step 3: Create test files

Create test classes following the convention `{ClassName}Test.java` in `src/test/java/` with matching package structure:

```
src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/
    ChainBreakMechanicTest.java
    CancelDamageMechanicTest.java
    XpBonusMechanicTest.java
    ... (one per mechanic)

src/test/java/io/github/chasehuegel/skilling/engine/requirements/
    RequirementEngineTest.java

src/test/java/io/github/chasehuegel/skilling/engine/feedback/
    LevelUpDispatcherTest.java
    FanfareDispatcherTest.java
    BossBarPoolTest.java

src/test/java/io/github/chasehuegel/skilling/engine/trigger/impl/
    BlockBreakTriggerTest.java
    RideHorseTriggerTest.java
    ... (one per trigger)
```

### Step 4: Implement key tests

**RequirementEngineTest:**
- `check()` passes when cooldown expires
- `check()` fails when on cooldown
- `check()` passes with valid states
- `check()` fails with missing states
- `check()` passes with item possession
- `check()` fails with missing item possession
- `consume()` deducts items
- `consume()` applies cooldown
- Full lifecycle: check → execute (mock) → consume

**Mechanic tests (representative sample):**
- `CancelDamageMechanic`: verify cancellation with chance
- `ChainBreakMechanic`: verify block break chain
- `DodgeMechanic`: verify dodge triggers correctly
- `XpBonusMechanic`: verify multiplier stored correctly (after ISSUE-027 implementation)
- `ApplyStatusMechanic`: verify effect applied, verify damager check

**LevelUpDispatcherTest:**
- `getLevelForXp()` returns correct level
- `showXpBossBar()` shows correct bar
- `broadcastLevelUp()` sends correct messages

## Self-Review

- This is the largest-scope issue on the list — estimated 80-120 test methods across 25+ test files
- Focus on high-value tests first: RequirementEngine lifecycle, then critical mechanics, then feedback
- Use parameterized tests for mechanics with configurable parameters (chance, multiplier, etc.)
- Integration-level tests (YAML parsing → execution) should be a separate effort
- Run `./gradlew test` after each batch of tests to verify nothing breaks
