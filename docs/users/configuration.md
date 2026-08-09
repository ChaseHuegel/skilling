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

### branding

Controls every in-game visual element. Templates use legacy color codes (`&0-&f`, `&l`, `&o`, `&#rrggbb` for hex) and `{placeholder}` tokens. Tokens whose values are unknown are left verbatim. `{color}` resolves to each skill's own `display.color` as a legacy code.

| Key | Type | Default | Description |
|---|---|---|---|
| `branding.skill_template` | string[] | see below | Full skill tooltip, one line per rendered lore line |
| `branding.bar_template.width` | int | `20` | XP bar character width (1-200) |
| `branding.bar_template.filled` | string | `&a█` | Per-unit filled bar string |
| `branding.bar_template.empty` | string | `&8█` | Per-unit empty bar string |
| `branding.bar_template.start` | string | `&7[` | Left bracket string |
| `branding.bar_template.end` | string | `&7]` | Right bracket string |
| `branding.abilities_template` | string[] | `["{ability}", ""]` | Repeated per ability. No separator is injected between blocks |
| `branding.ability_type_template.active` | string | `&8Active` | `{type}` for active abilities |
| `branding.ability_type_template.passive` | string | `&8Passive` | `{type}` for passive abilities |
| `branding.ability_locked_template` | string[] | see below | Template for locked abilities |
| `branding.ability_unlocked_template` | string[] | see below | Template for unlocked abilities |
| `branding.level_up.title` | string | `&6Level up!` | Level-up title |
| `branding.level_up.subtitle` | string | `{color}{name} &aincreased to {level}` | Level-up subtitle |
| `branding.level_up.message` | string | `&fYou leveled up &a[{name} {level}]` | Level-up chat message |
| `branding.level_up.maxed_message` | string | `&f{player} has reached max level {color}[{name}]` | Broadcast at max level |
| `branding.ability_unlock.title` | string | `&6Unlocked!` | Ability-unlock title |
| `branding.ability_unlock.subtitle` | string | `&a✔ {name} &8· {type}` | Ability-unlock subtitle |
| `branding.ability_unlock.message` | string | `&fYou unlocked the ability &a[{name} &8· {type}&a]` | Ability-unlock chat message |
| `branding.ability_feedback.ready_message` | string | `&a✦ {color}{name} &ais ready!` | Cooldown-ready chat + action bar |
| `branding.gui.title` | string | `&6Skills` | Flat-layout chest title (paginated titles come from `gui.yml`) |
| `branding.gui.prev_page` | string | `&6◀ Prev Page` | Previous-page arrow name |
| `branding.gui.next_page` | string | `&6Next Page ▶` | Next-page arrow name |
| `branding.gui.page_count` | string | `&7{count} skill(s)` | Page indicator count line |
| `branding.gui.skill_name_unlocked` | string | `&a{name}` | Unlocked skill icon name |
| `branding.gui.skill_name_locked` | string | `&7{name} &8· Locked` | Locked skill icon name |
| `branding.guide_book.name` | string | `&6Skills Guide` | Guide book display name |
| `branding.guide_book.lore` | string | `&7Right-click to open your skills` | Guide book lore line |
| `branding.boss_bar.title_format` | string | `{color}{name} &7- &f{level}` | Boss bar title text |
| `branding.boss_bar.default_color` | string | `white` | `BarColor` name for pool-created bars |
| `branding.boss_bar.default_style` | string | `solid` | `BarStyle` name for pool-created bars |
| `branding.command.header` | string | `&6=== Skills Commands ===` | `/skills help` header |
| `branding.command.command` | string | `&e{command}` | Help command line |
| `branding.command.description` | string | `&f{description}` | Help description |
| `branding.command.usage` | string | `&eUsage: {usage}` | Usage line |
| `branding.command.success` | string | `&a{message}` | Success feedback |
| `branding.command.error` | string | `&c{message}` | Error feedback |
| `branding.command.info` | string | `&7{message}` | Informational feedback |

**Placeholders**

| Token | Available in | Meaning |
|---|---|---|
| `{level}` | skill_template, level_up | Current level |
| `{max_level}` | skill_template | Skill max level |
| `{bar}` | skill_template | Rendered XP bar |
| `{xp_into}` | skill_template | XP into the current level |
| `{xp_needed}` | skill_template | XP needed for the next level (equals `{xp_into}` at max level, so it reads `XP: 5 / 5`) |
| `{xp_total}` | skill_template | Total XP |
| `{color}` | most templates | The skill's own `display.color` as a legacy code |
| `{lore}` | skill_template, ability templates | Skill/ability lore lines. A line holding `{lore}` is dropped when empty |
| `{abilities}` | skill_template | `abilities_template` repeated per ability. Dropped when the skill has none |
| `{ability}` | abilities_template | The locked or unlocked ability template block |
| `{name}` | ability templates, level_up, ability_unlock, gui, ready_message | Skill or ability display name |
| `{type}` | ability templates, ability_unlock | `Active`/`Passive` from `ability_type_template` |
| `{player}` | level_up | The leveling player's name |
| `{count}` | gui.page_count | Number of skills on the page |
| `{into}` / `{needed}` | boss_bar.title_format | XP into / needed for the current level |
| `{command}` / `{description}` / `{usage}` / `{title}` | command templates | Command feedback pieces |

Defaults reproduce the plugin's original look. The engine renders templates verbatim. No spacing or separators are injected between lines or ability blocks, so blank lines in the templates are intentional. `bar_template.width` must be 1-200, bar strings must be non-blank, and `boss_bar` color/style must be valid Bukkit `BarColor`/`BarStyle` names. Invalid values fail fast on load/reload.

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
| `web.allowed_origins` | list | `[]` | Cross-origin origins allowed to read the admin API. The frontend is served same-origin, so this is normally empty. Add entries (e.g. `http://localhost:5173` for the Vite dev server) only when accessing from another origin. Unlisted origins get no `Access-Control-Allow-Origin` header and are blocked. |

The web GUI uses Basic auth over plaintext HTTP, so credentials are base64-encoded, not encrypted. Put the GUI behind a TLS-terminating reverse proxy (nginx, Caddy) or bind to `127.0.0.1` in production. Failed logins are rate-limited per client IP (locked out after 10 failures within 15 minutes). Changing `web.port`, `web.username`, or `web.password` requires editing `config.yml` and restarting the server. The web UI rejects such changes with a "requires restart" message. Saving config from the web UI preserves every key the editor does not model (e.g. `setup.first_run`). Only the edited keys are written.

## tags/

The `tags/` data folder defines custom namespaced tag groups referenced in
skill YAML files. The plugin scans the folder recursively on startup and on
`/skills reload`. Every `.yml` file in the folder (including subfolders)
loads in sorted relative-path order.

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

**Additive merge:** when two files define the same tag key, the entry lists
are appended together. The first file's entries do not overwrite the second
file's entries. Cross-file `#c:` references resolve in one pass, so a file can
reference a tag that another file defines.

**Warn-and-skip:** a file that cannot be read as tags (non-map YAML, a scalar
where a list is expected, an unknown material or entity name, or an unknown
vanilla tag) logs a warning and is skipped. It does not fail the load.

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

The shipped `tags/base.yml` defines these custom tags (used by the bundled
skill YAMLs):

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

> **Web GUI:** The Tags page edits both `custom_tags` and `entity_tags` in
> `tags/base.yml`. Each kind is shown in its own section ("Material Tags" and
> "Entity Tags"); a save stages both sections in one API call, and an invalid
> entity value (unknown entity type or vanilla tag) is rejected with a clear
> error instead of being staged.
