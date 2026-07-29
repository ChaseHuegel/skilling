# `/skills reload` does not persist `TagResolver` into `SkillManager`

## Issue

When `/skills reload` is executed, `LockdownManager.reload()` creates a fresh `CustomTagLoader` and `TagResolver` from `tags.yml`, but this new `TagResolver` is **never stored back** into the `SkillManager` (or `RequirementEngine` or `SkillEventListener`). All three components continue referencing their original `TagResolver` backed by the stale `CustomTagLoader`, so changes to `tags.yml` are invisible after reload until the server is fully restarted.

**ISSUES.md reference:** Line 114–115

## Root Cause

In `LockdownManager.reload()` (Phase 4, lines 74–78):

```java
var customTagLoader = new CustomTagLoader();
customTagLoader.load(new File(plugin.getDataFolder(), "tags.yml"));
var tagResolver = new TagResolver(customTagLoader);
skillManager.clear();
skillManager.loadSkills(new File(plugin.getDataFolder(), "skills"));
```

The new `TagResolver` is constructed but then dropped — never assigned back to `SkillManager`, `RequirementEngine`, or `SkillEventListener`. All three components have `final` fields for `TagResolver` with no setter, and:

1. **SkillManager** holds a `final TagResolver tagResolver` (line 32) — `SkillManager.clear()` + `loadSkills()` re-parses YAMLs but the new resolver is not injected.
2. **RequirementEngine** holds a `final TagResolver tagResolver` (line 25) — used for item matching at runtime.
3. **SkillEventListener** holds a `final TagResolver tagResolver` (line 62 or similar) — used for filter matching in event handlers.
4. **`Skilling.customTagLoader`** field (line 67) is also never reassigned, so startup-time instance persists.

Since `SkillManager.parseFilter()` stores raw strings in `Filter` records and resolution happens lazily at runtime via `SkillEventListener.matchFilter()` and `RequirementEngine.hasItem()`, the stale resolver is used for all gameplay tag lookups after reload until a full server restart.

## Affected Files

| File | Path | Role |
|------|------|------|
| `LockdownManager.java` | `engine/lockdown/LockdownManager.java` | Creates TagResolver but never persists it |
| `SkillManager.java` | `engine/SkillManager.java` | `final TagResolver tagResolver` field — no setter |
| `RequirementEngine.java` | `engine/requirements/RequirementEngine.java` | `final TagResolver tagResolver` field — no setter |
| `SkillEventListener.java` | `engine/listener/SkillEventListener.java` | `final TagResolver tagResolver` field — no setter |
| `Skilling.java` | `Skilling.java` | Stores `customTagLoader` field, holds references to all above |

## Development Plan

### Step 1: Add `TagResolver` setter to `SkillManager`

- Change `SkillManager.tagResolver` from `final` to non-final (`private TagResolver tagResolver`)
- Add a public setter: `public void setTagResolver(TagResolver tagResolver)`

### Step 2: Add `TagResolver` setter to `RequirementEngine`

- Change `RequirementEngine.tagResolver` from `final` to non-final
- Add a public setter: `public void setTagResolver(TagResolver tagResolver)`

### Step 3: Add `TagResolver` setter to `SkillEventListener`

- Change `SkillEventListener.tagResolver` from `final` to non-final
- Add a public setter: `public void setTagResolver(TagResolver tagResolver)`

### Step 4: Update `LockdownManager.reload()` to persist the new resolver

After creating the new `tagResolver` in Phase 4, inject it into all three consumers:

```java
// After creating tagResolver:
skillManager.setTagResolver(tagResolver);
plugin.getRequirementEngine().setTagResolver(tagResolver);
plugin.getSkillEventListener().setTagResolver(tagResolver);
plugin.setCustomTagLoader(customTagLoader);  // add setter in Skilling.java
```

### Step 5: Verify

- Add a custom tag to `tags.yml` (e.g., `#c:test: [minecraft:stone]`)
- Create a skill filter referencing `#c:test`
- Run `/skills reload`
- Verify the filter resolves correctly at runtime (e.g., break a stone block)
- Run without the custom tag and confirm resolution fails cleanly

### Testing

- **Manual test:** Start server, modify `tags.yml`, run `/skills reload`, verify tag-dependent filters work with the updated definitions.
- **Unit test:** Create a `LockdownManagerTest` that verifies the `TagResolver` injection propagates to all three consumers after reload.

## Self-Review

- The fix is minimal and non-invasive — no structural changes, just adding setters and the injection call in the reload flow.
- All three consumers already receive `TagResolver` at construction time; adding a setter with the same field type is safe.
- Thread safety: `TagResolver` and `CustomTagLoader` are effectively immutable after load (both store their data in `final` fields / `EnumMap`), so replacing the reference atomically is safe even if accessed off the main thread (though all three consumers are primarily used on the Bukkit main thread).
- The `Skilling.customTagLoader` setter ensures the "startup stats" log references the current loader if that code path is ever re-run.
- No change to the `Skilling.onEnable()` flow — that already works correctly.

## Future Considerations

- If more components are added that consume `TagResolver`, they should follow the same pattern: accept it at construction AND provide a setter for reload injection.
- Consider centralizing the `TagResolver` instance on `Skilling` itself with a getter, so components look it up dynamically rather than storing their own reference. This would eliminate the need for per-component setters.
