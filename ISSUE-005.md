# ISSUE-005: Runtime Config Modification (`/skills set`)

## Description

Allow administrators to modify config values at runtime via
`/skills set <key> <value>`. Changes apply immediately (update
the in-memory setting) and persist to `config.yml` on disk.

## Scope

- New subcommand: `/skills set <key> <value>`
- The `<key>` argument resolves to a YAML path in `config.yml`
  (e.g., `debug_logging`, `titles.stay_duration`, `global_xp_modifier`)
- The `<value>` argument auto-detects type (boolean, int, double, string)
- Changes are saved to disk immediately
- Changes apply in-memory immediately (calls `reloadConfigSettings()` 
  or targeted setter)
- Tab completion for known config keys
- Only the `skilling.admin` permission

## Excluded from scope

- Adding new keys to `config.yml` (that's editing the default resource)
- Validation of business-logic constraints (e.g., negative durations)
  — YAML path must exist in the loaded config

## Proposed Solution

### Cloud command registration

```java
commandManager.command(commandManager.commandBuilder("skills")
    .literal("set")
    .permission("skilling.admin")
    .required("key", new ConfigKeyParser())
    .required("value", StringParser.stringParser())
    .handler(ctx -> {
        String key = ctx.get("key");
        String value = ctx.get("value");
        
        // Update the in-memory config
        plugin.getConfig().set(key, parseValue(value));
        plugin.saveConfig();
        
        // Re-read config settings into memory
        plugin.reloadConfigSettings();
        
        ctx.sender().source().sendMessage(
            Component.text("Set " + key + " to " + value, NamedTextColor.GREEN));
    }));
```

### ConfigKeyParser

Custom Cloud argument parser that:
- Provides tab completion for known config keys
- Validates the key exists in the current config (fail-fast)

Known keys to suggest:
- `debug_logging` (boolean)
- `titles.stay_duration` (int, ms)
- `global_xp_modifier` (double)
- `database.pool_size` (int)
- `bossbar.max_active` (int)
- `bossbar.fade_ticks` (int)
- `debouncer.interval_ms` (int)

### Value type detection

```java
private static Object parseValue(String raw) {
    if (raw.equalsIgnoreCase("true")) return true;
    if (raw.equalsIgnoreCase("false")) return false;
    try {
        if (raw.contains(".")) return Double.parseDouble(raw);
        return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
        return raw;
    }
}
```

### Persistence

`plugin.saveConfig()` writes the current in-memory config to disk.
This is a blocking I/O operation but is acceptable for an admin
command (rarely invoked, small file).

### Steps

1. Create `ConfigKeyParser` implementing `ArgumentParser<C, String>`
   and `BlockingSuggestionProvider<C>`.
2. Register `/skills set` command in `SkillsCommand.register()`.
3. Verify `plugin.saveConfig()` and `reloadConfigSettings()` 
   correctly persist and re-read the value.
4. Build, test, commit.

## Risk

Low. `saveConfig()` is a standard Bukkit API call. The only risk
is a malformed config file on disk, which `reloadConfigSettings()`
handles gracefully (falls back to defaults).
