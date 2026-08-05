# ISSUE-249: Configurable In-Game Branding

## Context & User Story
- **Goal:** As a server admin, I want to configure every in-game visual element of Skilling from `config.yml`. These elements are the skill tooltip lore, XP bar, ability lines, level-up/unlock messaging, chest titles, guide book, boss bar text, and command feedback. This keeps my server's branding consistent and fully under my control.
- **Agent Role:** You are an expert backend engineer executing this task. Styling is authored with inline legacy `&` codes in YAML templates plus a `{color}` token that resolves to each skill's own `display.color`. Templates render verbatim. The plugin adds no hardcoded spacing or separators between template blocks.

## Implementation Requirements
- [x] Add a `branding:` section to `config.yml` with fully commented documentation (per `src/AGENTS.md` §8).
- [x] `branding.skill_template` renders the whole skill tooltip. Supports `{level} {max_level} {bar} {xp_into} {xp_needed} {xp_total} {color} {lore} {abilities}`.
- [x] `branding.bar_template` drives the XP bar (width 1-200, per-unit `filled`/`empty`, `start`/`end` bracket strings, each carrying inline `&` codes).
- [x] `branding.abilities_template` is repeated per ability with no hardcoded separators. `{ability}` expands to the locked or unlocked ability template.
- [x] `branding.ability_locked_template` / `ability_unlocked_template` support `{name} {level} {type} {lore}`. `{type}` resolves via `ability_type_template.active`/`.passive` using the existing active/passive heuristic.
- [x] Maxed semantics: templates render verbatim. At max level `{xp_needed}` equals `{xp_into}` (reads `XP: 5 / 5`) and the bar renders full. Locked-ability `{lore}` is injected verbatim (no dimming).
- [x] `{color}` resolves the skill's `display.color` to a legacy code (all named colors + `#rrggbb`), falling back to `&f`.
- [x] `branding.level_up` (title/subtitle/message/maxed_message) and `branding.ability_unlock` (title/subtitle/message) drive level-up and unlock feedback. `branding.ability_feedback.ready_message` drives the ability-ready chat + action bar.
- [x] Extended surfaces: `branding.gui` (flat chest title, nav arrows, page count, unlocked/locked skill icon names), `branding.guide_book` (name/lore), `branding.boss_bar` (title format + default BarColor/BarStyle), `branding.command` (header/command/description/usage/success/error/info).
- [x] Branding is re-parsed on `/skills reload` and web Apply & Reload (`reloadConfigSettings()`). UI caches invalidate as they already do.
- [x] Web admin GUI models the `branding` keys: `ConfigHandler.get/update/validate` + `ConfigPage.vue` sections. Structural validation mirrors the engine.
- [x] Fail-fast parsing: wrong types, out-of-range width, blank bar strings, and invalid `BarColor`/`BarStyle` names throw during load/reload. Unresolved placeholders stay verbatim (visible in-game), consistent with `LoreResolver`.

## Technical Specifications & Context
- **Target Files:**
  - New: `src/main/java/io/github/chasehuegel/skilling/engine/ui/branding/BrandingConfig.java`, `TemplateRenderer.java`, `SkillColorCode.java`
  - Modified: `Skilling.java`, `engine/ui/SkillMenuBuilder.java`, `engine/feedback/LevelUpDispatcher.java`, `engine/listener/SkillEventListener.java`, `engine/ui/SkillsGuideBook.java`, `engine/feedback/BossBarPool.java`, `engine/command/SkillsCommand.java`, `src/main/resources/config.yml`, `web/handler/ConfigHandler.java`, `web/frontend/src/views/ConfigPage.vue`
  - Docs: `docs/users/configuration.md`, `docs/dev/DESIGN.md`, `src/AGENTS.md`
- **Dependencies:** existing `LoreResolver`, `resolveAbilityLore`, active/passive heuristic in `formatAbilityLine`, `resolveBarColor`/`mmColorName` in `LevelUpDispatcher`.
- **Constraints:** Per `src/AGENTS.md`: fail-fast parsing. Component API / legacy `&` deserialization via `LegacyComponentSerializer.legacyAmpersand()` (hex-capable). No hardcoded spacing between template blocks. Thread-safe (immutable config, volatile swap). Web package must stay uncoupled from engine classes (`web/AGENTS.md`).

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including new tests: `BrandingConfigTest`, `TemplateRendererTest`, `SkillMenuBuilderLoreTest`, `ConfigHandlerBrandingTest`.
- [x] `cd web/frontend && npm run build` passes (type-check gate).
- [x] Defaults in `branding:` reproduce today's exact look (green level/bar/abilities, aqua XP numbers, gold titles, gray secondary text).
- [x] Maxed skill shows full bar and `XP: {into} / {into}`. `{lore}`/`{abilities}` lines drop when empty. `{bar}` honors custom width/chars/brackets. `{color}` resolves named + hex skill colors.
- [x] DOX pass complete (config.yml comments, `docs/users/configuration.md`, `docs/dev/DESIGN.md`, `src/AGENTS.md` ownership updated).
