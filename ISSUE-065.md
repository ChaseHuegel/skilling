# ISSUE-065: Level-Up Command Execution

## Description

Provide a mechanism for skill designers to specify console commands that
execute automatically when a player levels up in a skill. These commands
support runtime placeholder substitution so designers can grant rewards,
trigger broadcasts, run economy scripts, or integrate with other plugins
without writing Java code.

## Scope

- **Plugin schema change:** New `level_up_commands` field in skill YAML
  definition (optional list of command strings).
- **Backend execution:** Run commands from console sender at level-up time,
  with placeholder resolution.
- **Web GUI support:** Add "Level-Up Commands" editor section to the
  skill editor page with an insertable placeholder menu.
- **Documentation:** Update `template-skill.yml` and `docs/creating-skills.md`.

No changes to the ability/mechanic/trigger system. Commands run *after*
the level-up event is fired and visual feedback is dispatched, as a
side-effect-free extension point.

## Proposed Solution

### New class

| File | Responsibility |
|------|---------------|
| `engine/LevelUpCommandExecutor.java` | Resolve placeholders and dispatch commands via console sender |

### Schema change: `SkillDefinition` record

Add a new component record to `SkillDefinition`:

```java
public record LevelUpCommand(
        String command    // raw command string with {placeholders}
) {}
```

Add a field to the top-level `SkillDefinition` record:

```java
public record SkillDefinition(
        String id,
        int maxLevel,
        Display display,
        Progression progression,
        List<XpSource> xpSources,
        List<Ability> abilities,
        List<LevelUpCommand> levelUpCommands   // NEW — default empty
) {}
```

### YAML schema (`template-skill.yml`)

New optional root-level list:

```yaml
# Commands executed as console on every level-up.
# Supports placeholders: {player}, {level}, {skill_id}, {skill_name}
level_up_commands:
  - "say {player} just reached level {level} in {skill_name}!"
  - "give {player} minecraft:diamond 1"
  - "eco give {player} {level}"
```

Placements are resolved at runtime, not parse time.

### Parsing (`SkillManager.parseSkill`)

In `SkillManager.parseSkill`, read the list:

```java
List<SkillDefinition.LevelUpCommand> levelUpCommands = parseLevelUpCommands(
        config.getList("level_up_commands"));
```

```java
private List<SkillDefinition.LevelUpCommand> parseLevelUpCommands(List<?> list) {
    if (list == null) return List.of();
    List<SkillDefinition.LevelUpCommand> cmds = new ArrayList<>();
    for (Object raw : list) {
        if (raw instanceof String s && !s.isBlank()) {
            cmds.add(new SkillDefinition.LevelUpCommand(s));
        }
    }
    return cmds;
}
```

### Execution: `LevelUpCommandExecutor`

```java
public final class LevelUpCommandExecutor {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{(player|level|skill_id|skill_name)\\}");

    public static void execute(SkillDefinition skill, Player player, int newLevel) {
        if (skill.levelUpCommands() == null || skill.levelUpCommands().isEmpty()) return;

        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skill.id();

        for (var entry : skill.levelUpCommands()) {
            String resolved = entry.command()
                    .replace("{player}", player.getName())
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{skill_id}", skill.id())
                    .replace("{skill_name}", displayName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }
    }
}
```

A simple string `replace` chain is preferred over regex for these four tokens
because it is O(n) and avoids recompilation overhead per command. The
`PLACEHOLDER_PATTERN` is kept as a class constant for potential future validation
or tooling use.

### Integration point: `SkillEventListener` (and `SkillsCommand`)

Insert a call to `LevelUpCommandExecutor.execute()` in **every** path that
calls `LevelUpDispatcher.broadcastLevelUp()`:

1. `SkillEventListener.java` line 322 — after `broadcastLevelUp(player, skill,
   newLevel)` and after the event is fired.
2. `SkillsCommand.java` line 281 — admin `/skills setlevel` path.
3. `SkillsCommand.java` line 342 — admin `/skills addxp` path.

The execution is synchronous on the main thread (console dispatch requires
it), which is acceptable because the operation is a trivial string replace +
dispatch. No DB/async work is involved.

### Thread safety

Console command dispatch must happen on the Bukkit main thread. The
integration points in `SkillEventListener` and `SkillsCommand` both run
on the main thread already, so no additional synchronization is needed.

### Database schema

No changes. Level-up commands are defined purely in YAML and never persisted
per-player.

### DTO changes (`SkillDetailDTO`)

Add a new `LevelUpCommandDTO` record:

```java
public record LevelUpCommandDTO(
        String command
) {}
```

Add field to `SkillDetailDTO`:

```java
List<LevelUpCommandDTO> levelUpCommands  // default empty
```

Update component record:

```diff
 public record SkillDetailDTO(
     String id,
     ...
-    List<AbilityDTO> abilities
+    List<AbilityDTO> abilities,
+    List<LevelUpCommandDTO> levelUpCommands
 ) {}
```

### Serializer changes (`SkillSerializer`)

In `fromMap`:

```java
List<SkillDetailDTO.LevelUpCommandDTO> commands = new ArrayList<>();
List<String> cmdRaw = listStr(raw, "level_up_commands");
if (cmdRaw != null) {
    for (String c : cmdRaw) {
        commands.add(new SkillDetailDTO.LevelUpCommandDTO(c));
    }
}
```

In `toMap`:

```java
if (dto.levelUpCommands() != null && !dto.levelUpCommands().isEmpty()) {
    root.put("level_up_commands",
            dto.levelUpCommands().stream().map(SkillDetailDTO.LevelUpCommandDTO::command).toList());
}
```

### Web GUI

#### Backend API

No new routes. The existing `GET/PUT /api/skills/{id}` endpoints
automatically include the new field through `SkillSerializer`.

#### Frontend component: `LevelUpCommandsSection.vue`

New component at `web/frontend/src/components/skills/LevelUpCommandsSection.vue`.

- Displays an editable list of command strings.
- Each row: text input + remove button.
- "Add Command" button appends a blank entry.
- Insertable placeholder chips/buttons below the list:
  - `{player}`, `{level}`, `{skill_id}`, `{skill_name}`
  - Clicking a chip inserts the placeholder at cursor position in the
    currently focused input (or appends it).
- Styled consistently with `DisplaySection.vue` (PrimeVue Aura variables,
  scoped CSS).

**Props:**

```typescript
defineProps<{
  modelValue: string[]  // v-model binding to parent
}>()
```

**Emits:**

```typescript
defineEmits<{
  'update:modelValue': [value: string[]]
}>()
```

#### Parent integration: `SkillEditorPage.vue`

Add the section between the ability editor and save/cancel buttons. The
Pinia skill staging store already manages the full `SkillDetailDTO` object,
so `levelUpCommands` flows through naturally.

## Implementation phases

### Phase 1: Backend schema + parsing

1. Add `LevelUpCommand` record to `SkillDefinition`.
2. Add `levelUpCommands` field to the `SkillDefinition` record.
3. Add `parseLevelUpCommands` to `SkillManager`.
4. Update `SkillManager.parseSkill` to read the field.
5. Update the `SkillDefinition` constructor call sites.
6. Create `LevelUpCommandExecutor`.
7. Wire into `SkillEventListener.broadcastLevelUp` and `SkillsCommand`.
8. Add commented example to `template-skill.yml`.
9. Build, test (`./gradlew build && ./gradlew test`).

### Phase 2: DTO + serializer

1. Add `LevelUpCommandDTO` record to `SkillDetailDTO`.
2. Add `levelUpCommands` field to `SkillDetailDTO`.
3. Update `SkillSerializer.fromMap`, `toMap`, `toYaml`, `fromYaml`.
4. Build.

### Phase 3: Web frontend

1. Create `LevelUpCommandsSection.vue`.
2. Integrate into `SkillEditorPage.vue`.
3. Verify round-trip (load skill → edit commands → save → reload matches).
4. Run Playwright E2E tests.

## Risk

Low. Commands are optional (default empty list), the feature is a pure
extension with no changes to existing parsing or execution paths, and
console dispatch is a well-understood Bukkit pattern. Replacing untrusted
user input into commands is by design — the console sender is trusted, and
the server admin controls the YAML content.
