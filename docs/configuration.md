# Configuration Reference

## config.yml

Global settings for the Skilling engine.

### database

| Key | Type | Default | Description |
|---|---|---|---|
| `database.pool_size` | int | `10` | Maximum connections in the HikariCP pool |
| `database.wal_mode` | bool | `true` | Enable SQLite Write-Ahead Logging for concurrent reads/writes |

### bossbar

| Key | Type | Default | Description |
|---|---|---|---|
| `bossbar.max_active` | int | `2` | Maximum visible Boss Bars per player (LRU eviction) |
| `bossbar.fade_ticks` | int | `40` | Tick duration for Boss Bar fade-out |

### debouncer

| Key | Type | Default | Description |
|---|---|---|---|
| `debouncer.interval_ms` | int | `500` | Minimum ms between repeated failure feedback per player-ability |

## tags.yml

Defines custom namespaced tag groups referenced in skill YAML files.

Tags use the `#c:` prefix and can contain:
- Direct material names: `minecraft:diamond`
- Cross-references to vanilla tags: `#minecraft:logs`

```yaml
custom_tags:
  c:ores:
    - "minecraft:iron_ore"
    - "minecraft:gold_ore"
    - "#minecraft:coal_ores"
```
