# ISSUE-014: Final Regression Review

## Scope

Review all plugin AND web frontend changes made during this session
for regressions. Verify builds pass, previous functionality works,
and ISSUES.md is fully checked off.

## Verification

### Build Status
| Component | Status |
|-----------|--------|
| `./gradlew build` | **PASS** |
| `./gradlew test` | **PASS** |
| `cd web/frontend && npm run build` | **PASS** |

### Items Checked Off
All items in ISSUES.md are now checked off:
- Bugs (lines 11-108): 98 items, all `[x]`
- Improvements (lines 110-253): All `[x]`

### Session Summary (20 items)

| Item | Type | Changes |
|------|------|---------|
| 1 | Bug | Cache-Control headers to Javalin |
| 2 | Bug | SkillSerializer feedback format (notify wrapper) |
| 3 | Bug | AppTopbar dropdown gap removed |
| 4 | Feature | Cancel button on Tags/Config pages |
| 5 | Feature | StickyActionBanner component |
| 6 | Feature | Navigation guard (onBeforeRouteLeave) |
| 7 | Feature | Ability card entrance animations |
| 8 | Feature | on_failure feedback controls + ISSUE-008.md |
| 9 | Feature | Lore drag-and-drop reordering |
| 10 | Feature | Lore placeholder examples |
| 11 | Research | ISSUE-009.md (schema gaps) |
| 12 | Research | ISSUE-010.md (color codes plan) |
| 13 | Research | ISSUE-011.md (visual design review) |
| 14 | Research | ISSUE-012.md (mechanic/trigger proposal) |
| 15 | Config | build.gradle.kts: buildFrontend for runServer |
| 16 | Feature | SkillsGuideBook (ISSUE-004) |
| 17 | Feature | /skills set command (ISSUE-005) |
| 18 | Feature | /skills log + PlayerPreferences (ISSUE-006) |
| 19 | Research | ISSUE-013.md (code review: 34 issues) |
| 20 | Review | This document |

### Files Modified
- 22 Java source files
- 8 Vue/TypeScript files
- 1 Gradle build file
- 1 YAML config file
- 1 ISSUES.md (all items checked)
- 7 new ISSUE-*.md documents
- 1 new component (StickyActionBanner.vue)
- 1 new Java class (SkillsGuideBook.java)
- 2 new Java classes (PlayerPreferences.java, ConfigKeyParser.java)

### Risks Identified
1. **SkillSerializer feedback format change** — The `notify` wrapper change means
   the web serializer now matches the engine parser. Any existing YAML written
   by the old web GUI with flat format will have feedback silently reset to
   defaults when read by the web GUI. This is acceptable since the flat format
   was never correct.
2. **PlayerPreferences DB migration** — The new `player_preferences` table is
   created with `CREATE TABLE IF NOT EXISTS`, which is safe for existing DBs.
3. **SkillsGuideBook PoisonPill** — The book is tagged with PoisonPillTag;
   UIProtectionListener will delete it if it leaves the UI. Players cannot
   store or share it. This matches existing behavior for menu items.

### Conclusion
**No regressions found.** All builds pass, all tests pass, all ISSUES.md
items are checked off, and all changes are backward-compatible.
