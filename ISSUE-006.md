# ISSUE-006: Per-Player Logging Preferences

## Description

Add personal per-player settings to control which notification
types appear in chat. Players can opt in/out of XP gain messages,
level-up messages, unlock messages, and ability activation messages
via `/skills log` subcommands.

## Scope

Four toggleable preferences stored per player:

| Command | Key | Default | Effect |
|---------|-----|---------|--------|
| `/skills log xp <true/false>` | `logXp` | `false` | Chat message on each XP gain |
| `/skills log levels <true/false>` | `logLevels` | `true` | Override level-up notification |
| `/skills log unlocks <true/false>` | `logUnlocks` | `true` | Override unlock notification |
| `/skills log abilities <true/false>` | `logAbilities` | `true` | Override ability activation message |

All four share the same infrastructure:
- Storage in `PlayerProfile` (in-memory) + database column
- Command registration under `/skills log`
- Gate checks in existing notification paths

## Excluded from scope

- Per-player toggles for action bar messages (chat only)
- Override of server-wide broadcasts (max level fanfare)
- GUI for settings (command-only for now)

## Proposed Solution

### 1. PlayerPreferences record

```java
// In engine/profile/PlayerPreferences.java
public record PlayerPreferences(
    boolean logXp,
    boolean logLevels,
    boolean logUnlocks,
    boolean logAbilities
) {
    public static final PlayerPreferences DEFAULTS =
        new PlayerPreferences(false, true, true, true);
}
```

### 2. Extend PlayerProfile

Add a `PlayerPreferences preferences` field with getter/setter.
Serialize to/from the database in a single text column (JSON)
or individual integer columns.

**Database migration:** Add a `preferences` column (TEXT, default `{}`)
to the `player_skills` or a new `player_preferences` table.
Since the existing schema uses composite keys, store as JSON:
`{"xp":false,"levels":true,"unlocks":true,"abilities":true}`

### 3. Command registration

```java
commandManager.command(commandManager.commandBuilder("skills")
    .literal("log")
    .permission("skilling.use")
    .required("type", LogTypeParser())  // suggests: xp, levels, unlocks, abilities
    .required("value", BooleanParser.booleanParser())
    .handler(ctx -> {
        Player player = (Player) ctx.sender().source();
        String type = ctx.get("type");
        boolean value = ctx.get("value");
        
        PlayerProfile profile = profileManager.getOrCreate(player);
        PlayerPreferences prefs = profile.getPreferences();
        PlayerPreferences updated = switch (type) {
            case "xp" -> prefs.withLogXp(value);
            case "levels" -> prefs.withLogLevels(value);
            case "unlocks" -> prefs.withLogUnlocks(value);
            case "abilities" -> prefs.withLogAbilities(value);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
        profile.setPreferences(updated);
        
        player.sendMessage(Component.text(
            "Set " + type + " logging to " + value, NamedTextColor.GREEN));
    }));
```

### 4. Gate notifications

In `grantXp()` (SkillEventListener.java line ~185):
```java
if (profile.getPreferences().logXp()) {
    player.sendMessage(...);
}
```

In `broadcastLevelUp()` (both files), skip chat message if
`!profile.getPreferences().logLevels()`.

In `fireAbilities()`, skip ability activation chat if
`!profile.getPreferences().logAbilities()`.

### Steps

1. Create `PlayerPreferences` record with defaults.
2. Add preferences field to `PlayerProfile` with JSON
   serialization/deserialization.
3. Add DB migration to add `preferences` column.
4. Update `ProfileManager.getOrCreate()` to load preferences
   from DB on hydration.
5. Register `/skills log` command tree.
6. Integrate gates into `grantXp()`, `broadcastLevelUp()`, 
   and `fireAbilities()`.
7. Build, test, commit.

## Risk

Low-Medium. DB migration requires schema change. Existing rows
get the default preferences via `DEFAULTS` when the column is
empty or missing. Rollback requires removing the column.
