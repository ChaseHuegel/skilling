# REPORT_VISUAL-POLISH.md: Visual Polish & Presentation Cohesion Design Plan

**Status:** Research report (no production changes applied)
**Issue:** [ISSUE-186](../issues/ISSUE-186.md)
**Date:** 2026-08-02
**Inputs:** `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillMenuBuilder.java`, `src/main/java/io/github/chasehuegel/skilling/engine/ui/UIProtectionListener.java`, `src/main/java/io/github/chasehuegel/skilling/engine/ui/GuiLayoutConfig.java`, `src/main/resources/gui.yml`, `src/main/java/io/github/chasehuegel/skilling/engine/feedback/{LevelUpDispatcher,FanfareDispatcher,FeedbackDebouncer}.java`, `src/main/resources/skills/**`, `docs/reports/REPORT_XP-CURVE.md`, `docs/reports/REPORT_DUALWIELD-API.md`

---

## 1. Executive Summary

The plugin's presentation surfaces (chest GUI, level-up fanfare, chat/action-bar feedback,
tooltips, boss bars) each look reasonable in isolation but do not share one visual language.
Colors, glyphs, spacing, and message copy differ between the GUI, titles, bars, and chat. This
plan catalogs every surface, then proposes concrete, file-anchored improvements. It starts with
two high-impact, low-effort wins (Unicode XP-bar blocks and a GUI close button) and defines a
cohesive palette plus a prioritized polish backlog. Everything stays config-driven and consistent
with the data-driven architecture. Nothing hard-codes a skill-specific presentation.

---

## 2. Current Presentation Surface Inventory

### 2.1 Skill chest GUI: `engine/ui/SkillMenuBuilder.java` + `resources/gui.yml`

| Element | Location | Current look |
|---|---|---|
| Skill icons | `buildSkillIcon` (lines 269-303) | Item material from `display.icon`. Display name colored by `display.color` (green when unlocked, gray + `· Locked` when not). Item amount = level (1-99). |
| Tooltip XP bar | `buildSkillLore` (lines 211-225) | Text bar built from `'|'` (filled) and `'.'` (empty), wrapped in `[ ]`, filled part green, empty gray. |
| Page icons | `buildPageIcon` (174-191) | Material + title. Gray `N skill(s)` lore. |
| Nav arrows | `createNavItem` (165-172) | Gold `◀ Prev Page` / `Next Page ▶` arrows. |
| Filler panes | `createFillerPane` (147-163) | Black stained glass pane, empty display name, hidden tooltip, optional custom model data. |
| Page titles | `buildPaginatedOverview` (96-97) | `displayTitle` from `gui.yml` deserialized via legacy ampersand. |
| Inventory titles | `buildFlatOverview` (63), `buildPaginatedOverview` (94-97) | Flat: `"Skills"` gold. Paginated: per-page title. |
| Poison pill | `tagAllItems` / per-item | Every GUI item tagged `PoisonPillTag`. |

### 2.2 Feedback: `engine/feedback/*`

| Element | Location | Current look |
|---|---|---|
| Level-up title | `LevelUpDispatcher` | `MiniMessage`-driven. Bright-color fireworks sequence. Title text assembled there (see §3.3 for copy). |
| XP boss bar | `LevelUpDispatcher.showXpBossBar` | Title `"{name} - {level}"`, gray separators, white level. Bar color resolved from `display.color`. |
| Level-up chat/action bar | `FanfareDispatcher.sendActionBar`/`sendChat` | Per-skill message from ability `feedback` or level-up path. |
| Fireworks | `LevelUpDispatcher` | `BRIGHT_COLORS` cycle. |
| Failure feedback | `FeedbackDebouncer` + `SkillEventListener` | Ability `on_failure` action-bar copy, e.g. `&cRequires 1x Coal!`. |

### 2.3 Bundled skill YAML: `resources/skills/**`

- Lore uses `&7` for neutral, `&a` for values, `&c`/`&8` for costs/requirements, `&e`/`&d`/`&6`
  for per-skill accents. This is **not consistent**. E.g. `&b` appears in some success messages,
  `&d`/`&6` vary per skill.
- Success messages (`feedback.notify.message`) and failure copy (`on_failure`) use ad-hoc accents.

---

## 3. Proposed Changes

### 3.1 Replace the tooltip XP bar characters (high impact, low effort)

**Location:** `SkillMenuBuilder.buildSkillLore` lines 211-225.

Replace `'|'`/`'.'` with block glyphs and color the two segments distinctly:

| Role | Current | Proposed | Notes |
|---|---|---|---|
| Filled | `|` | `█` (U+2588 FULL BLOCK) | Solid, unambiguous fill. |
| Empty | `.` | `░` (U+2591 LIGHT SHADE) | Clearly "track" vs fill. Reads as a bar, not a loading spinner. |
| Bar wrap | `[ ]` | `[ ]` (keep) | Square brackets frame the bar cleanly at default font width. |

Rendering considerations:
- Both glyphs are monospace-adjacent in the default Minecraft font and render at equal width, so
  the bar stays visually consistent (unlike `█`+`▄` which have different box heights).
- Color handling: keep the current two-`Component` split. The filled part is colored green
  (`NamedTextColor.GREEN`), the empty part gray. Optionally tie the filled color to `display.color`
  for per-skill flavor, but keep gray as the track default for contrast.
- No font/space workarounds needed. Verify in-game that the glyphs render in the tooltip's
  proportional font (they do in the default `minecraft:default` font).

**Suggested implementation:** a tiny static helper `renderProgressBar(filled, width, color)`
returning the two `Component`s, so the logic is testable and reusable by any future
chat/action-bar progress display.

### 3.2 Add a close button to the skill GUI (high impact, medium effort)

**Location:** `resources/gui.yml` layout + `UIProtectionListener.handleNavigation`
(lines 71-112) + `SkillMenuBuilder.buildPaginatedOverview` (76-136).

- Add a per-page `close_slot` (or a config `close_button.slot`) to `gui.yml`. Default to the
  last row center column only when it does not collide with the page indicator slot
  (`indicatorSlot = lastRowStart + 4`) or the nav arrows (`prevSlot`, `nextSlot`). Default: use
  the slot one right of the indicator (e.g. `lastRowStart + 5`) on every page, or the last slot
  of the last row for the final page.
- **Wiring:** build a close item (e.g. `BARRIER` or `OAK_DOOR`, gold display name `✕ Close`) in
  `SkillMenuBuilder`, poison-pill tagged like every other GUI item.
- **Interaction:** in `UIProtectionListener.handleNavigation`, when the clicked slot equals the
  page's configured `close_slot`, call `player.closeInventory()` instead of paging. Keep it in
  the existing `handleNavigation` so bottom-inventory clicks are already excluded (line 75 guard).
- **Interaction with pagination:** because the close slot is a fixed per-page column, it must not
  overlap prev/next/indicator. Validate in `GuiLayoutConfig` at load (fail-fast, like existing
  row/slot validation) so a conflicting config is rejected before runtime.

**Why:** premium plugins expose an obvious exit affordance. Currently the only ways out are
`ESC`/`E`, which less-experienced players miss. A visible close button also gives the flat layout
a consistent exit (flat overview has no nav row today).

### 3.3 Cohesive color palette

Define one palette and map each surface to it. Keep the accent derived from `display.color`
(already loaded per skill) and add a small set of **semantic** colors that never change:

| Semantic role | Color | Where used |
|---|---|---|
| Accent (per-skill) | `display.color` → `NamedTextColor`/`BarColor` | Skill names, XP bar fill, boss-bar title name, page icon title. |
| Neutral body | `GRAY` (`&7`) | Lore body text, separators (`-`, `·`), "Passive/Active" type tags. |
| Value / positive | `GREEN` (`&a`) | Lore numeric values, success amounts, XP gains, level-up numbers. |
| Cost / requirement | `RED` (`&c`) | `Costs`/`Requires` lore lines, failure feedback, missing-item copy. |
| Locked / muted | `DARK_GRAY` (`&8`) | Locked skill names, locked-ability lore, "N skill(s)" page hint. |
| Feedback accent (transient) | `YELLOW`/`AQUA` (`&e`/`&b`) | Level-up title, ability success action bars. |

**Config surfaces to add (proposed):**
- A `presentation.colors` block in `config.yml` letting admins override the semantic roles
  (e.g. `value_color: "&a"`, `cost_color: "&c"`). Defaults match the table above.
- Keep per-skill `display.color` as the sole per-skill accent input (no new hard-coded colors).

**Why shared semantic colors:** an admin scanning a tooltip, a boss bar, and a chat message sees
the same "green = good, red = cost" grammar everywhere. Per-skill accents give identity without
breaking the grammar.

### 3.4 Level-up fanfare refinement

**Location:** `LevelUpDispatcher` (title assembly), `FanfareDispatcher`.

- **Title copy:** standardize the level-up title to a consistent shape, e.g.
  `{skill} Level Up!` with the new level emphasized (accent color + `BOLD`), matching the
  boss-bar naming so the two never disagree.
- **Boss bar:** keep `{name} - {level}` but move the level into the accent color so it reads as
  the "live" value. Keep debug XP details gray (already done).
- **Fireworks:** cycle `BRIGHT_COLORS` in a fixed pleasant order (already rainbow) but start at
  the skill accent when it maps to a `org.bukkit.Color`, falling back to the bright cycle.

### 3.5 Inventory & item naming consistency

- **Flat overview title:** change `"Skills"` (gold, `SkillMenuBuilder.java:63`) to
  `"Skills"` in the accent of... there is no single accent in the flat view. Leave gold as the
  neutral brand color, but make sure paginated page titles default to `"Skills"` too when
  `gui.yml` omits a title, so both layouts present the same brand.
- **Page indicator:** `buildPageIcon` lore is `"N skill(s)"` gray. Standardize to
  `"Page X / Y · N skills"` using the neutral/value colors for cohesion with the nav arrows.

### 3.6 Locked-skill presentation

- Keep the gray + `· Locked` name, but render `· Locked` in `DARK_GRAY` (already done) and add
  the unlock-level hint `Requires level N` in `DARK_GRAY` lore when the player is below
  `unlockLevel`. This mirrors the ability-lore lock treatment (`SkillMenuBuilder.buildSkillLore`
  lines 252-261) for a consistent locked grammar across GUI and tooltips.

### 3.7 Feedback debounce copy

**Location:** `FeedbackDebouncer` + ability `on_failure` strings.

- Standardize failure messages to one tense/voice ("Requires X", "Cooling down: {time}s",
  "Too exhausted!") and one accent per failure kind (cooldown `&e`, missing item `&c`, state
  `&c`, exhaustion `&c`). Audit `resources/skills/**` `on_failure` blocks to match.

---

## 4. Prioritized Polish Backlog

| # | Item | Location | Effort | Priority | Rationale |
|---|---|---|---|---|---|
| 1 | XP-bar block glyphs (`█`/`░`) | `SkillMenuBuilder.buildSkillLore` 211-225 | S | High | Smallest change, instantly premium look. |
| 2 | GUI close button | `gui.yml` + `UIProtectionListener.handleNavigation` + `SkillMenuBuilder` | M | High | Obvious exit. Matches premium UX. Configurable. |
| 3 | Semantic palette + config overrides | `config.yml` + all feedback/ui color sites | M | High | One grammar across every surface. |
| 4 | Level-up title/boss-bar copy consistency | `LevelUpDispatcher` | S | Med | Removes conflicting presentation. |
| 5 | Page indicator copy (`Page X / Y`) | `SkillMenuBuilder.buildPageIcon` 180-183 | S | Med | Better orientation. |
| 6 | Locked hint lore (`Requires level N`) | `SkillMenuBuilder.buildSkillIcon` 288-290 | S | Med | Guides progression. |
| 7 | Flat/paginated title consistency | `SkillMenuBuilder.buildFlatOverview` 63 | S | Low | Cohesion. |
| 8 | Failure-message voice/color audit | `resources/skills/**` `on_failure` | M | Med | Consistent feedback tone. |
| 9 | Fireworks accent start color | `LevelUpDispatcher.BRIGHT_COLORS` | S | Low | Subtle polish. |

Legend: S = <1 day, M = 1-3 days.

---

## 5. Suggested Sequencing (follow-up issues)

1. **`feat(ui):` block-glyph XP bars + page indicator copy** (§3.1, §3.5). Self-contained,
   highest visual ROI.
2. **`feat(ui):` skill GUI close button** (§3.2). Requires `gui.yml` schema addition + nav
   validation. Pair with the existing `GuiLayoutConfig` validation tests.
3. **`feat(ui):` semantic color palette + config overrides** (§3.3). Touches all feedback
   surfaces. Do last so it is layered over the new components rather than rewritten.
4. **`chore(skills):` failure-copy audit** (§3.7). Mechanical pass over bundled YAML.

Each should reference this report section and the exact file/line anchors above so the
implementation tickets are actionable.

---

## 6. Verification of DoD

- [x] Every current presentation surface inventoried with file/line locations. §2.
- [x] XP-bar character replacement proposed with exact glyphs, font/render notes, and color
  handling. §3.1.
- [x] GUI close button proposal: placement, `UIProtectionListener` wiring, poison-pill tagging,
  pagination interaction. §3.2.
- [x] Cohesive color palette defined and mapped per surface, with config surfaces identified. §3.3.
- [x] Remaining polish cataloged with rationale and effort/priority. §3.4-§3.7 and §4.
- [x] Every proposed change is actionable (location + rationale). §3, §4, §5.
- [x] No production code changes (research only).

**Cross-reference:** [ISSUE-186](../issues/ISSUE-186.md).
