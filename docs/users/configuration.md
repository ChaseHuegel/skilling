# Configuration Reference

## config.yml

Global settings for the Skilling engine.

### database

| Key | Type | Default | Description |
|---|---|---|---|
| `database.pool_size` | int | `10` | Maximum connections in the HikariCP pool |
| `database.wal_mode` | bool | `true` | Enable SQLite Write-Ahead Logging for concurrent reads/writes |

### titles

| Key | Type | Default | Description |
|---|---|---|---|
| `titles.stay_duration` | int | `5000` | Milliseconds a title message remains visible before fading |

### bossbar

| Key | Type | Default | Description |
|---|---|---|---|
| `bossbar.max_active` | int | `2` | Maximum visible Boss Bars per player (LRU eviction) |
| `bossbar.fade_ticks` | int | `40` | Tick duration for Boss Bar fade-out |

### debouncer

| Key | Type | Default | Description |
|---|---|---|---|
| `debouncer.interval_ms` | int | `500` | Minimum ms between repeated failure feedback per player-ability |

### global_xp_modifier

| Key | Type | Default | Description |
|---|---|---|---|
| `global_xp_modifier` | double | `1.0` | Global XP multiplier applied to all XP gains across all skills |

### debug_logging

| Key | Type | Default | Description |
|---|---|---|---|
| `debug_logging` | bool | `false` | Verbose console logging for runtime troubleshooting (XP grants, filter passes/fails, trigger dispatch) |

### crop_grow

| Key | Type | Default | Description |
|---|---|---|---|
| `crop_grow.search_radius` | int | `10` | Radius in blocks to search for nearby players when a crop grows naturally |

### skills_guide_book

| Key | Type | Default | Description |
|---|---|---|---|
| `skills_guide_book.enabled` | bool | `true` | Enable the craftable Skills Guide book that opens the GUI on right-click |

## tags.yml

Defines custom namespaced tag groups referenced in skill YAML files.

Tags use the `#c:` prefix and can contain:
- Direct material names: `minecraft:diamond`
- Cross-references to vanilla tags: `#minecraft:logs`
- Cross-references to other custom tags: `#c:stone` (custom tags resolve transitively)

```yaml
custom_tags:
  ores:
    - "minecraft:iron_ore"
    - "minecraft:gold_ore"
    - "#minecraft:coal_ores"
```

The shipped `tags.yml` defines these custom tags (used by the bundled skill YAMLs):

| Tag | Members |
|---|---|
| `#c:ores` | All coal/iron/gold/diamond/emerald/redstone/lapis/copper/nether ores |
| `#c:logs` | `#minecraft:logs` |
| `#c:gems` | `minecraft:diamond`, `minecraft:emerald`, `minecraft:amethyst_shard` |
| `#c:stone` | `#minecraft:stone_crafting_materials`, `#minecraft:base_stone_overworld`, `#minecraft:base_stone_nether` |
| `#c:excavatable` | Dirt, grass, gravel, sand, clay, soul sand/soil, podzol, mycelium, rooted dirt |
| `#c:veinminer` | `#c:stone`, `#c:ores` |
| `#c:stone_products` | Decorative stone blocks, walls, slabs, stairs, glass |
| `#c:herbs` | `#minecraft:flowers`, `#minecraft:tall_flowers`, sugar cane, cactus, mushrooms, etc. |
| `#c:heavy_weapons` | Iron/diamond/netherite swords and axes, mace |
| `#c:light_weapons` | Wooden/stone/golden swords, trident |
| `#c:tools` | `#minecraft:pickaxes/axes/shovels/hoes/swords`, bows, crossbows, trident, mace, armor pieces |
| `#c:leather_armor` | Leather helmet/chestplate/leggings/boots |
| `#c:bows` | `minecraft:bow`, `minecraft:crossbow` |
| `#c:instruments` | `minecraft:goat_horn` (Bard abilities) |
| `#c:fishing_rods` | `minecraft:fishing_rod` |
