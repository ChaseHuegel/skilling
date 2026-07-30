# ISSUE-066: Skill Lore Lines

## Description

Add a configurable `display.lore` field to the skill-level YAML schema
so skill definitions can carry descriptive lore lines — similar to how
abilities carry `display.lore`. These lines render in the chest GUI
tooltip (before abilities) and in the web GUI skill cards and editor
header card. The web GUI reuses the same lore line editor component
pattern from the ability lore editor, providing a consistent editing
experience for designers.

## Scope

- **Backend YAML schema:** New optional `display.lore` field (list of
  strings) at the skill level, identical shape to ability `display.lore`.
- **Engine parsing:** Parse and store the lore list in `SkillDefinition`.
- **Chest GUI:** Render skill lore lines in the skill icon tooltip,
  positioned between the XP bar/info section and the ability list.
- **Placeholder resolution:** Route skill lore through `LoreResolver`
  with skill-level placeholders (`{level}`, `{max_level}`, `{skill_name}`).
- **REST DTO:** Expose lore as a `List<String>` in `SkillDetailDTO`.
- **Serializer:** Read/write `display.lore` in YAML round-trip.
- **Frontend — Dashboard:** Show truncated lore lines in `SkillCard`.
- **Frontend — Editor:** Add a "Skill Lore" editor section to the
  `DisplaySection` card (or as a standalone section) that reuses the
  exact same lore-line editing UI: drag-to-reorder, AppCombobox with
  placeholder suggestions, `&` color preview, add/remove.
- **Frontend — Editor header card:** Display the rendered lore below the
  banner meta line.
- **No new abilities/mechanics:** Pure presentation layer change.

## Proposed Solution

### 1. Backend: SkillDefinition.Display — new `lore` field

Add a `List<String> lore` field to the `SkillDefinition.Display` record
in `SkillDefinition.java`. Default to an empty list when absent in YAML.

```java
public record Display(
    String name,
    String icon,
    int customModelData,
    String color,
    String style,
    List<String> lore
) {}
```

All existing call-sites that construct `Display` (e.g. `SkillManager`
line 120) must be updated to pass the new `lore` argument.

### 2. Backend: SkillManager — parse `display.lore`

In `SkillManager.parseDisplay()`, read a string list from the
`display.lore` YAML path:

```java
@SuppressWarnings("unchecked")
List<String> lore = (List<String>) section.getList("lore", List.of());
```

The fallback `parseDisplay(null)` constructor also needs to include
`List.of()`.

### 3. Backend: SkillMenuBuilder — render lore in tooltip

In `SkillMenuBuilder.buildSkillLore()`, after the XP progress line
and before the ability loop, iterate the skill's `display.lore()` lines
and resolve them through `LoreResolver.resolveAll()`. The evaluator map
for skill-level placeholders is constructed from built-in tokens:

| Placeholder       | Value                         |
|-------------------|-------------------------------|
| `{level}`         | Player's current skill level  |
| `{max_level}`     | `skill.maxLevel()`            |
| `{skill_name}`    | `skill.display().name()`      |
| `{xp}`            | Player's total XP in skill    |

```java
// After XP section, before abilities
if (!skill.display().lore().isEmpty()) {
    lore.add(Component.empty());
    Map<String, ParameterEvaluator> skillParams = Map.of(
        "level",       new ConstantEvaluator(level),
        "max_level",   new ConstantEvaluator(skill.maxLevel()),
        "skill_name",  new ConstantEvaluator(0),  // unused; name injected literally
        "xp",          new ConstantEvaluator(currentXp)
    );
    List<String> resolved = LoreResolver.resolveAll(
        skill.display().lore(), skillParams, level, 0);
    for (String line : resolved) {
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
    }
}
```

### 4. Backend: REST API — `SkillDetailDTO`

Add a `List<String> lore` field to `SkillDetailDTO`:

```java
public record SkillDetailDTO(
    String id,
    String displayName,
    int maxLevel,
    String icon,
    int customModelData,
    String color,
    String style,
    List<String> lore,             // <-- new
    ProgressionDTO progression,
    List<XpSourceDTO> xpSources,
    List<AbilityDTO> abilities
) {}
```

### 5. Backend: SkillSerializer — round-trip `display.lore`

**fromMap():** Read `display.lore` from the raw map when constructing
the DTO.

**toMap():** Write `display.lore` back into the `display` map under the
`lore` key, omitting it when empty (matching the pattern used for
ability lore in `abilityToMap()`).

### 6. Frontend: SkillCard — display lore on dashboard

Add a `lore` prop (or `description` field) to `SkillCard.vue`. Render
the first 1–2 lines below the `skill-meta` line, truncated with an
ellipsis if long. Use `v-html` with the same `renderFormattedText()`
utility that renders `&` color codes (already used by `renderedLore()`
in `AbilitiesSection.vue`).

```typescript
// Props
skill: {
    // ...existing fields
    lore?: string[];
};
```

```vue
<div v-if="skill.lore?.length" class="skill-lore-preview">
    <div
        v-for="(line, i) in skill.lore.slice(0, 2)"
        :key="i"
        class="skill-lore-line"
        v-html="renderFormattedText(parseAmpersandCodes(line))"
    />
    <div v-if="skill.lore.length > 2" class="skill-lore-more">
        +{{ skill.lore.length - 2 }} more lines
    </div>
</div>
```

The `SkillCard` type and the dashboard page's API calls must also be
updated to pass/expect the `lore` array. The skills store's skill
summary type should include `lore?: string[]`.

### 7. Frontend: DisplaySection — add lore editor

In `DisplaySection.vue`, add a collapsible "Skill Lore" block at the
bottom that reuses the same editing pattern from `AbilitiesSection.vue`
(lines 678–727):

- `section-block section-block--lore` wrapper
- Drag-to-reorder using `dragstart`/`dragover`/`dragend` handlers
- `AppCombobox` per line (with suggestion popup showing `{level}`,
  `{max_level}`, `{skill_name}`, `{xp}`)
- Inline `renderedLore()` preview below each input
- Add / remove buttons
- Lore lines stored in `modelValue.lore` (extending `DisplayConfig`
  interface)

This means `DisplayConfig` in `DisplaySection.vue` gains:
```typescript
interface DisplayConfig {
  icon: string
  customModelData: number
  color: string
  style: string
  lore: string[]
}
```

And the `DisplaySection` emits `update:modelValue` which already
propagates via `v-model` on the `SkillEditorPage`.

### 8. Frontend: SkillEditorPage — integrate lore into form and banner

**Form init:** Add `lore: []` to the default `form` reactive object.

**API conversion:** The `apiAbilityToForm`/`formAbilityToApi` pattern
already flattens `display.lore` into `lore` for abilities. For the
skill-level DTO, `lore` is a direct field on `SkillDetailDTO`, so no
flattening is needed — it maps directly.

**Save payload:** The `save()` function already includes `form` fields
via object spread, so `form.lore` flows into the payload automatically.

**Banner preview:** In the editor banner, render skill lore lines
below the `banner-meta` line as a rendered preview:

```vue
<div v-if="form.lore?.length" class="banner-lore">
    <div
        v-for="(line, i) in form.lore"
        :key="i"
        class="banner-lore-line"
        v-html="renderFormattedText(parseAmpersandCodes(line))"
    />
</div>
```

### 9. Frontend: SkillCard on SkillEditorPage

The skill card at `/skills` (dashboard) uses `SkillCard.vue` which
receives skill summary data from `GET /api/skills`. The summary API
should include `lore` (or the frontend shapes it from the full detail
DTO). The Pinia skills store's fetch method should be updated to
capture `lore` if present.

### 10. YAML template documentation

In `template-skill.yml`, add a commented `lore` block inside the
`display:` section showing usage and supported placeholders:

```yaml
display:
  name: "Mining"
  icon: "minecraft:iron_pickaxe"
  custom_model_data: 1001
  color: "GREEN"
  style: "SEGMENTED_10"
  # lore:
  #   - "&7Master the art of mining deep underground."
  #   - "&7Current level: &a{level}&7/&a{max_level}"
  #   - ""
  #   - "&8Add blank lines with an empty quoted string."
```

## New / Modified Files

### Modified

| File | Change |
|------|--------|
| `skilling-api/src/main/java/.../engine/SkillDefinition.java` | Add `List<String> lore` to `Display` record |
| `skilling-api/src/main/java/.../engine/SkillManager.java` | Parse `lore` in `parseDisplay()` |
| `skilling-api/src/main/java/.../engine/ui/SkillMenuBuilder.java` | Render resolved lore in `buildSkillLore()` |
| `skilling-api/src/main/java/.../web/dto/SkillDetailDTO.java` | Add `List<String> lore` field |
| `skilling-api/src/main/java/.../web/dto/SkillSerializer.java` | Read/write `display.lore` in `fromMap()`/`toMap()` |
| `src/main/resources/template-skill.yml` | Add documented `lore` example in `display` section |
| `web/frontend/src/components/skills/SkillCard.vue` | Render lore lines on dashboard cards |
| `web/frontend/src/components/skills/DisplaySection.vue` | Add lore editor UI (collapsible, drag-reorder, combobox, preview) |
| `web/frontend/src/views/SkillEditorPage.vue` | Add `lore` to form defaults, APi mapping, and banner preview |

### No new files

All changes are additive within existing files. No new classes or
components are introduced.

## Implementation Phases

### Phase 1: Backend Schema + DTO (1 commit)

1. Add `List<String> lore` to `SkillDefinition.Display` record.
2. Update all `Display` constructor call-sites:
   - `SkillManager.parseDisplay()` — parse from `section.getList("lore")`
   - `SkillManager.parseDisplay(null)` fallback — pass `List.of()`
   - Any inline `new SkillDefinition.Display(...)` in tests.
3. Update `SkillMenuBuilder.buildSkillLore()` — inject resolved lore
   lines between XP section and ability list.
4. Add `List<String> lore` to `SkillDetailDTO` record.
5. Update `SkillSerializer.fromMap()` to read `display.lore`.
6. Update `SkillSerializer.toMap()` to write `display.lore` under
   the display map.

### Phase 2: Frontend — Display Section + Editor (1 commit)

1. Extend `DisplayConfig` interface in `DisplaySection.vue` with `lore`.
2. Implement the collapsible lore editor block in `DisplaySection.vue`
   (drag-to-reorder, AppCombobox, preview, add/remove — mirroring
   `AbilitiesSection.vue` lines 678–727).
3. Update `SkillEditorPage.vue`:
   - Add `lore: []` to default `form`.
   - Render lore preview in the editor banner.
   - Verify save payload includes `lore`.

### Phase 3: Frontend — Dashboard display (1 commit)

1. Add `lore` to the `SkillCard` props interface.
2. Render up to 2 lore lines (with `v-html` for color codes) below
   `skill-meta`, with a "+N more lines" indicator for long lore.
3. Update the Pinia skills store or API client to include `lore` in
   skill summaries.

### Phase 4: Template + Docs (1 commit)

1. Add commented `lore` example to `template-skill.yml` `display:`
   section.
2. Update `docs/creating-skills.md` with skill lore schema
   documentation.
3. Update `docs/configuration.md` with the new display option.
4. Build, test (`./gradlew build` + `./gradlew test`), self-review.

## Risk

Low. All changes are additive — no existing skill definitions are
affected (empty lore produces no visual change). The lore editor is a
direct copy of an existing, well-tested pattern. No new registry
entries or runtime effects.
