# Minor Polish: `crop_grow` Trigger Verification and PlaceholderAPI Expansion

## Issue

Two polish items identified during the comprehensive review (ISSUE-013 and this review pass):

### 1. `crop_grow` Trigger — Stub Verification

The `SkillEventListener.onCropGrow()` handler was documented as a stub with an empty body in ISSUE-013 (line 105 — "`SkillEventListener.onCropGrow()` — event handler registered but body is empty; `crop_grow` trigger will never fire"). While the ISSUE-013 checklist is marked as resolved, it's unclear if `crop_grow` was ever implemented or if the event wiring is complete.

The `crop_grow` trigger has a test file (`SkillTriggerTest.java` from ISSUE-055) but the actual event listener body needs verification. If it's still a no-op, XP sources, filters, and abilities keyed to `crop_grow` will silently never fire.

### 2. PlaceholderAPI Expansion for Evaluator Output (Post-ISSUE-059)

After ISSUE-059 adds the basic `%skilling_level_{skill}%` placeholders, there's a natural follow-up: expose **evaluator output** so server owners can display dynamic ability parameters in scoreboards and chat. For example:

- `%skilling_evaluator_mining_geologist_multiplier%` — Current yield multiplier for Geologist
- `%skilling_evaluator_mining_vein_miner_chain_limit%` — Current chain limit for Vein Miner

This was discussed in ISSUE-059's "Future Considerations" but is valuable enough to plan explicitly.

## Affected Files

| File | Action |
|------|--------|
| `engine/listener/SkillEventListener.java` | Verify `onCropGrow()` implementation, add handler logic if missing |
| `engine/trigger/impl/CropGrowTrigger.java` | Verify trigger wiring |
| `engine/integration/PlaceholderAPIHook.java` | Add evaluator placeholder expansion |
| `src/test/java/.../SkillEventListenerTest.java` | Add test for crop_grow handler |
| `src/test/java/.../PlaceholderAPIHookTest.java` | Add tests for evaluator placeholders |
| `docs/capabilities.md` | Update `crop_grow` documentation status |
| `docs/api-integration.md` | Document evaluator placeholders |

## Development Plan

### Step 1: Verify and fix `crop_grow` trigger

**Background:** The `crop_grow` trigger maps to `BlockGrowEvent` (or `CropGrowEvent` in Paper API — need to verify the exact event class). When a crop grows naturally (not player-placed), this event fires. The trigger should grant XP to players within a configured radius (since crops grow naturally without a player initiating the action).

**Current state (to verify):**
- `SkillEventListener.onCropGrow()` method exists and is registered as a listener
- The method body may be empty (`// TODO` or `{}`)
- The `crop_grow` trigger exists in `TriggerRegistry` and has a `CropGrowTrigger` implementation

**If empty, implement:**

```java
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onCropGrow(BlockGrowEvent event) {
    if (reloading) return;
    
    Block block = event.getBlock();
    Material type = block.getType();
    
    // Find nearest player within configured range (default: 10 blocks)
    // This is necessary because BlockGrowEvent has no direct player source
    Location cropLoc = block.getLocation();
    Player nearest = findNearestPlayer(cropLoc, 10);
    if (nearest == null) return;
    
    // Process XP sources for this player and crop type
    UUID playerId = nearest.getUniqueId();
    PlayerProfile profile = profileManager.get(playerId);
    if (profile == null || !profile.isInitialized()) return;
    
    for (Map.Entry<String, Long> entry : profile.getXpSnapshot().entrySet()) {
        String skillId = entry.getKey();
        SkillDefinition skill = skillManager.getSkill(skillId);
        if (skill == null) continue;
        
        for (XpSource source : skill.xpSources()) {
            if (!"crop_grow".equals(source.trigger())) continue;
            if (!matchFilter(nearest, source.filters(), type, null, null)) continue;
            
            int currentLevel = skill.getLevelForXp(entry.getValue());
            long reward = (long) source.reward().evaluate(currentLevel, 1);
            profile.addXp(skillId, reward);
            
            if (debugLogging) {
                // Log XP gain details
            }
        }
    }
}

private Player findNearestPlayer(Location location, double radius) {
    double radiusSq = radius * radius;
    Player nearest = null;
    double nearestDistSq = Double.MAX_VALUE;
    
    for (Player player : location.getWorld().getPlayers()) {
        double distSq = player.getLocation().distanceSquared(location);
        if (distSq < radiusSq && distSq < nearestDistSq) {
            nearestDistSq = distSq;
            nearest = player;
        }
    }
    return nearest;
}
```

**Key design decisions:**
- `BlockGrowEvent` has no direct player association — we find the nearest player
- Radius should be configurable (add a `crop_grow_radius` config key in `config.yml`, defaulting to 10 blocks)
- Use `EventPriority.MONITOR` and `ignoreCancelled = true` to avoid interfering with other plugins
- The `findNearestPlayer()` method should be efficient — use `distanceSquared` and only scan the crop's world

**Trigger configuration considerations:**
- Since crops grow naturally, the trigger applies XP to the nearest player within radius
- This means multiple players near the same crop field can all receive XP from different crops growing
- This also means XP can be earned without actively farming (standing near a farm) — which is intentional for passive playstyles

### Step 2: Add evaluator placeholders to PAPI hook

After ISSUE-059 adds PlaceholderAPI support, extend the hook with evaluator placeholders:

```java
@Override
public String onPlaceholderRequest(Player player, String params) {
    if (player == null || params == null) return "";

    String[] parts = params.split("_", 3);
    if (parts.length < 3) return "";

    String action = parts[0];        // "evaluator" (new action type)
    String skillId = parts[1];       // "mining"
    String rest = parts[2];          // "geologist_multiplier"

    SkillDefinition skill = plugin.getSkillManager().getSkill(skillId);
    if (skill == null) return "0";

    // Parse ability and parameter from rest
    // Format: evaluator_{skill}_{ability}_{parameter}
    // Example: %skilling_evaluator_mining_geologist_multiplier%
    
    // Find the ability and parameter
    for (Ability ability : skill.abilities()) {
        String abilityKey = ability.id().replace('-', '_');
        if (!rest.startsWith(abilityKey)) continue;
        
        String paramName = rest.substring(abilityKey.length() + 1);  // "multiplier"
        
        // Find the mechanic entry with this parameter
        for (MechanicEntry entry : ability.mechanics()) {
            Object paramValue = entry.parameters().get(paramName);
            if (paramValue instanceof ParameterEvaluator evaluator) {
                PlayerProfile profile = plugin.getProfileManager().get(player.getUniqueId());
                if (profile == null) return "0";
                
                long xp = profile.getXp(skillId);
                int currentLevel = skill.getLevelForXp(xp);
                double result = evaluator.evaluate(currentLevel, ability.unlockLevel());
                return String.format("%.2f", result);
            }
        }
    }
    
    return "0";
}
```

**Supported evaluator placeholders:**
- `%skilling_evaluator_mining_geologist_multiplier%` — Current yield multiplier for Geologist
- `%skilling_evaluator_mining_vein_miner_chain_limit%` — Current chain limit for Vein Miner
- `%skilling_evaluator_woodcutting_timber_feller_chain_limit%` — etc.

**Format:** `%skilling_evaluator_{skill_id}_{ability_id}_{parameter_name}%`

The placeholder path uses `{skill_id}_{ability_id}_{parameter_name}` joined by underscores. For ability IDs with hyphens, hyphens are replaced with underscores before matching.

This must NOT regress the existing placeholder evaluation.

### Step 3: Add config key for crop_grow radius

In `config.yml`:

```yaml
# Crop grow trigger configuration
# Radius (in blocks) to search for nearby players when a crop grows naturally
crop_grow:
  search_radius: 10  # Default: 10 blocks
  enabled: true
```

Add corresponding constant in `Skilling.java`:

```java
private int cropGrowRadius = 10;
private boolean cropGrowEnabled = true;

// In loadConfig():
cropGrowRadius = getConfig().getInt("crop_grow.search_radius", 10);
cropGrowEnabled = getConfig().getBoolean("crop_grow.enabled", true);
```

### Step 4: Update documentation

**`docs/capabilities.md`:**

Under "Triggers" section, update `crop_grow`:

```markdown
| Trigger | Event | Description | Status |
|---------|-------|-------------|--------|
| `crop_grow` | `BlockGrowEvent` | Fires when a crop grows naturally. XP is awarded to the nearest player within the configured `crop_grow.search_radius` (default: 10 blocks). | Implemented |
```

**`docs/api-integration.md`:**

Add evaluator placeholders to the placeholder table:

```markdown
| `%skilling_evaluator_{skill}_{ability}_{param}%` | Dynamic evaluator output | `34.50` |
```

### Step 5: Testing

**crop_grow trigger:**
- Unit test: Mock `BlockGrowEvent`, verify `onCropGrow()` calls `profile.addXp()` with correct values
- Edge case: No player within radius → no XP granted
- Edge case: Multiple players within radius → nearest player gets XP
- Integration test: Start server, place crops, wait for growth, verify XP gain

**Evaluator placeholders:**
- Unit test: Set player level, verify placeholder resolves to correct evaluator output
- Edge case: Unknown ability → returns "0"
- Edge case: Unknown parameter → returns "0"
- Edge case: Player has no profile → returns "0"

## Self-Review

- **crop_grow scope is small** — one event handler method, one config key, negligible performance impact (only fires when crops grow, which is relatively rare)
- **findNearestPlayer()** is O(n) where n = number of players in the world — acceptable for crop growth frequency
- **Evaluator placeholders** follow the same `%skilling_*%` pattern established in ISSUE-059
- **No breaking changes** — existing configurations continue to work; `crop_grow` trigger was previously a no-op, now it has behavior
- The crop_grow radius default of 10 blocks is reasonable — large enough to cover a typical farm plot, small enough to avoid granting XP to players in entirely different areas
- Evaluator placeholder format (underscore-joined) is simple but could conflict if skill IDs, ability IDs, or parameter names contain underscores. Mitigation: validate that skill/ability IDs use hyphens (YAML convention) while parameter names use underscores (Java convention), making conflicts unlikely.

## Future Considerations

- **crop_grow player tracking:** Instead of "nearest player," track which player planted the crop (requires listening to `BlockPlaceEvent` for seeds and storing planter UUID). This would be more accurate but requires persistent storage of planter data.
- **crop_grow radius per world:** Some servers may want different radii for different worlds (e.g., 5 blocks in the overworld, 20 in the end)
- **Advanced evaluator placeholders:** Consider `%skilling_evaluator_{skill}_{ability}_{param}_raw%` for unformatted double values vs. formatted strings
- **Tab completion:** Add evaluator placeholder names to PlaceholderAPI tab completion
