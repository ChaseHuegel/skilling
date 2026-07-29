# `LevelUpTrigger` maps to `PlayerLevelChangeEvent` (vanilla XP) — will never fire for Skilling's custom skill XP

## Issue

`LevelUpTrigger.getEventClass()` returns `PlayerLevelChangeEvent` (line 16). This event fires when a player's VANILLA Minecraft experience level changes (the XP bar above the hotbar). Skilling uses its own custom XP system (`PlayerProfile.addXp()`), which never triggers `PlayerLevelChangeEvent`. Therefore, the `level_up` trigger can never fire for Skilling's skill level-ups.

**ISSUES.md reference:** Line 129

## Root Cause

The trigger maps to a vanilla Minecraft event that Skilling's XP system doesn't interact with. Skilling manages its own XP via `PlayerProfile` / `SkillEventListener.grantXp()`, which calls `broadcastLevelUp()` directly rather than through any Bukkit event.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `LevelUpTrigger.java` | `engine/trigger/impl/LevelUpTrigger.java` | 16 |
| `SkillEventListener.java` | `engine/listener/SkillEventListener.java` | 305-306 (level-up detection, should fire trigger) |

## Development Plan

### Step 1: Design approach

**Option A: Custom Skilling event**
- Create a new `SkillingLevelUpEvent` that `SkillEventListener.grantXp()` fires when a level-up is detected
- `LevelUpTrigger` returns this custom event class
- This is clean, extensible, and allows other plugins to hook into Skilling level-ups

**Option B: Fire trigger directly**
- Instead of an event, detect level-ups in the trigger pipeline
- This requires refactoring the trigger system to be callable without a Bukkit event

### Step 2: Implement Option A (recommended)

Create `SkillingLevelUpEvent`:

```java
public class SkillingLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String skillId;
    private final int newLevel;

    // constructor, getters, HandlerList boilerplate
}
```

In `SkillEventListener.grantXp()`, after detecting a level-up:
```java
if (newLevel > oldLevel) {
    SkillingLevelUpEvent levelUpEvent = new SkillingLevelUpEvent(player, skill.id(), newLevel);
    Bukkit.getPluginManager().callEvent(levelUpEvent);
    broadcastLevelUp(player, skill, newLevel);
}
```

Update `LevelUpTrigger`:
```java
@Override
public Class<? extends Event> getEventClass() { return SkillingLevelUpEvent.class; }
```

### Step 3: Update `SkillEventListener` to handle the event

The `FireAbilities` path in `SkillEventListener` listens for Bukkit events. If a `SkillingLevelUpEvent` is fired, it should be handled by the `level_up` trigger's ability execution path.

## Self-Review

- Option A is the correct solution — creates a proper event bus integration
- `PlayerLevelChangeEvent` is completely unrelated to Skilling XP and should never have been used
- The custom event approach aligns with standard Bukkit plugin patterns
- Tests should verify the event fires and the trigger responds to it
