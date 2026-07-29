# ISSUE-008: `on_failure` Feedback Controls in Skill Editor

## Description

Add `on_failure` feedback override controls to the ability editor card in
the web skill editor. Currently, the engine's `SkillDefinition.Ability` record
includes `OnFailure onFailure` (a map of failure reason → `FailureFeedback`
with `actionBar` message and `sounds` list), but the web GUI has no UI to
configure it.

## Changes Required

### 1. Backend DTO — Add `OnFailureDTO` and `FailureFeedbackDTO`

Add new records to `SkillDetailDTO.java`:
```java
public record FailureFeedbackDTO(
        String actionBar,
        List<Map<String, Object>> sounds
) {}
public record OnFailureDTO(
        Map<String, FailureFeedbackDTO> reasons
) {}
```

Add `OnFailureDTO onFailure` field to `AbilityDTO`:
```java
public record AbilityDTO(
        // ... existing fields
        OnFailureDTO onFailure,
        FeedbackDTO feedback
) {}
```

### 2. Backend Serializer — Read/write `on_failure` YAML

In `SkillSerializer.parseAbility()`:
- Read `map(raw, "on_failure")`
- For each entry, parse `action_bar` and `sounds` into `FailureFeedbackDTO`
- Build `OnFailureDTO` with populated map

In `SkillSerializer.abilityToMap()`:
- If `a.onFailure()` is non-null and has any reasons, write an `on_failure` block
- For each reason, write `action_bar` and `sounds`

### 3. Frontend Types — Add `onFailure` to `Ability` interface

In `AbilitiesSection.vue`:
```typescript
interface FailureFeedback {
  actionBar: string
  sounds: SoundConfig[]
}
interface OnFailure {
  reasons: Record<string, FailureFeedback>
}
```

Add `onFailure?: OnFailure` to the `Ability` interface.

### 4. Frontend Form — Add `onFailure` default and form API passthrough

- Add empty `onFailure: { reasons: {} }` to the new ability factory
- In `SkillEditorPage.vue`, ensure `apiAbilityToForm` and `formAbilityToApi`
  pass `onFailure` through (it uses `...ab` spread, so it will pass through
  automatically if the backend sends it)

### 5. Frontend UI — Add `on_failure` section to ability card

Add a new collapsible sub-section after the Feedback section in
`AbilitiesSection.vue`:

```
┌─ On Failure ─────────────────────────────────┐
│  Failure Reason: cooldown                     │
│  ├─ Action Bar: "&cCooling down: {time}s"    │
│  └─ Sounds: [...same sound controls...]       │
│                                               │
│  Failure Reason: missing_item                 │
│  ├─ Action Bar: "&cRequires {item}"           │
│  └─ Sounds: [...same sound controls...]       │
│                                               │
│  [+ Add Failure Reason]                       │
└───────────────────────────────────────────────┘
```

- Use the same sound editor controls/patterns as the main feedback section
- Pre-populate known failure reason keys:
  - `cooldown` (time-based cooldown active)
  - `missing_item` (required item not found)
  - `missing_state` (required state not met)

### 6. YAML Schema

The output format matches the template YAML:
```yaml
on_failure:
  cooldown:
    action_bar: "&cCooling down: {time}s"
    sounds:
      - type: "BLOCK_NOTE_BLOCK_BASS"
        volume: 1.0
        pitch: 0.5
  missing_item:
    action_bar: "&cRequires {amount}x {item}"
```

## Risks

- Low. Adding new fields to DTOs is backward-compatible (older YAML without
  `on_failure` defaults to empty map, engine handles this fine).
- Existing ability YAML files with `on_failure` data will now be visible and
  editable in the web GUI instead of being silently lost on round-trip.
