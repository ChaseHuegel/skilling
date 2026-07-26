# Configuration Reference

## config.yml

Global plugin settings generated at `plugins/Skilling/config.yml`.

```yaml
# Database connection pool settings
database:
  # Maximum concurrent connections to SQLite
  # Higher values help under heavy concurrent load
  pool_size: 10

  # Write-Ahead Logging — allows concurrent reads during writes
  # Recommended: true (disabling may cause lock contention)
  wal_mode: true

# Boss Bar display settings
bossbar:
  # Maximum active boss bars visible on screen at once
  # LRU eviction drops the oldest bar when exceeded
  max_active: 2

  # Tick duration for the fade-out animation
  fade_ticks: 40

# Failure feedback throttling
debouncer:
  # Minimum milliseconds between repeated failure messages
  # Prevents client-side spam when abilities are on cooldown
  interval_ms: 500
```

### Key Reference

| Key | Type | Default | Description |
|---|---|---|---|
| `database.pool_size` | int | `10` | HikariCP connection pool size |
| `database.wal_mode` | bool | `true` | Enable SQLite WAL journal mode |
| `bossbar.max_active` | int | `2` | Max concurrent boss bars per player |
| `bossbar.fade_ticks` | int | `40` | Boss bar fade-out duration in ticks |
| `debouncer.interval_ms` | int | `500` | Feedback throttle interval in milliseconds |

---

## tags.yml

Custom tag definitions generated at `plugins/Skilling/tags.yml`. Tags let you group materials for use in skill filters with the `#c:` prefix (e.g., `#c:ores`).

```yaml
# Each top-level key under custom_tags becomes a resolvable namespace
custom_tags:
  # Use: target: "#c:ores" in skill YAML
  c:ores:
    - "minecraft:coal_ore"
    - "minecraft:iron_ore"
    - "#minecraft:copper_ores"   # Vanilla tags can be cross-referenced

  # Use: target: "#c:logs"
  c:logs:
    - "#minecraft:logs"           # Resolves to all vanilla log types
    - "minecraft:mangrove_roots"  # Custom additions not covered by the vanilla tag

  # Use: target: "#c:gems"
  c:gems:
    - "minecraft:diamond"
    - "minecraft:emerald"
    - "minecraft:amethyst_shard"
```

### Rules

- Entries can be individual materials (`minecraft:iron_ore`) or vanilla tag references (`#minecraft:logs`).
- Vanilla tags are resolved using Bukkit's `Tag` API and merged with the custom list.
- All tags are flattened into `EnumSet<Material>` at plugin load for O(1) lookup performance.
- The `#c:` prefix is reserved for custom tags. Vanilla tags use `#minecraft:`.

---

## Skill YAML Files

Skill definitions live in `plugins/Skilling/skills/`. Each file defines one skill. See [Creating Skills](creating-skills.md) for the full schema.
