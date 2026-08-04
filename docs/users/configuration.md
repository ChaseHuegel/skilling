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

### web

| Key | Type | Default | Description |
|---|---|---|---|
| `web.enabled` | bool | `false` | Enable the embedded admin web GUI. When first enabled with the shipped default password, the plugin generates a random password and logs it to the console once. |
| `web.port` | int | `8082` | Port the web GUI listens on |
| `web.bind_address` | string | `0.0.0.0` | Address to bind. Use `127.0.0.1` to restrict the GUI to this machine only (recommended without a reverse proxy). |
| `web.username` | string | `admin` | Basic auth username |
| `web.password` | string | `skilling` | Basic auth password. The shipped default is replaced by a generated random password the first time the web GUI is enabled. |
| `web.behind_proxy` | bool | `false` | Set `true` when a trusted reverse proxy (nginx, Caddy) sits in front and sets `X-Forwarded-For`. Rate limiting then keys on the real client IP (the right-most forwarded entry) instead of the proxy's address, so one client's failed attempts cannot lock out everyone behind the proxy. |
| `web.allowed_origins` | list | `[]` | Cross-origin origins allowed to read the admin API. The frontend is served same-origin, so this is normally empty; add entries (e.g. `http://localhost:5173` for the Vite dev server) only when accessing from another origin. Unlisted origins get no `Access-Control-Allow-Origin` header and are blocked. |

The web GUI uses Basic auth over plaintext HTTP, so credentials are base64-encoded, not encrypted. Put the GUI behind a TLS-terminating reverse proxy (nginx, Caddy) or bind to `127.0.0.1` in production. Failed logins are rate-limited per client IP (locked out after 10 failures within 15 minutes). Changing `web.port`, `web.username`, or `web.password` requires editing `config.yml` and restarting the server; the web UI rejects such changes with a "requires restart" message. Saving config from the web UI preserves every key the editor does not model (e.g. `setup.first_run`); only the edited keys are written.

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

**Entity tags** live under a separate `entity_tags:` key and are used by the
`target_type` state filter. They hold entity type names and/or vanilla entity
tag cross-references:

```yaml
entity_tags:
  undead:
    - "#minecraft:zombies"
    - "#minecraft:skeletons"
    - "minecraft:wither_skeleton"
```

Usage in a skill: `state: "target_type:#c:undead"`.

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
| `#c:light_armor` | `#c:leather_armor` (armor-gating tier) |
| `#c:medium_armor` | Chainmail, iron, golden, and turtle armor (armor-gating tier) |
| `#c:heavy_armor` | Diamond and netherite armor (armor-gating tier) |
| `#c:unarmored` | Empty slots (`minecraft:air`), elytra, and helmet-slot headwear |
| `#c:bows` | `minecraft:bow`, `minecraft:crossbow` |
| `#c:instruments` | `minecraft:goat_horn` (Bard abilities) |
| `#c:fishing_rods` | `minecraft:fishing_rod` |
| `#c:holy_blocks` | Beacon, conduit, lanterns, glowstone, gold block, `#minecraft:candles` (Piety) |
| `#c:crops` | `#minecraft:crops`, sugar cane, melon, pumpkin, cocoa |
| `#c:foods` | Cooked meats, baked potato (cooking sources) |
| `#c:potions` | Potion, splash/lingering potions, honey bottle |
| `#c:shields` | `minecraft:shield` |
| `#c:wooden_products` | `#minecraft:planks`, wooden slabs/stairs, crafting table, chest, ladder |
| `#c:shovels` | `#minecraft:shovels` |

| Entity tag | Members |
|---|---|
| `#c:undead` | `#minecraft:zombies`, `#minecraft:skeletons`, wither skeleton, phantom, zombified piglin, drowned, stray, husk |

> **Web GUI:** The Tags page edits `custom_tags` only. `entity_tags` are read-only
> in the GUI, displayed for reference, and preserved verbatim when tags are saved
> through the API.
