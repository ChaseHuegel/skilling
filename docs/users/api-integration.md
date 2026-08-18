# API Integration Guide

Skilling exposes a Bukkit `ServicesManager` API for addon plugins to register custom mechanics, triggers, and evaluators.

## Adding Skilling as a Dependency

Addon developers should depend on the slim `skilling-api` module, which contains only the public API interfaces and records with zero runtime dependencies.

### Maven

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.chasehuegel</groupId>
    <artifactId>skilling-api</artifactId>
    <version>main-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Gradle

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.chasehuegel:skilling-api:main-SNAPSHOT")
}
```

### Full Plugin Dependency (if you need internals)

If you need access to internal classes (e.g., for integration testing), depend on the full plugin JAR:

```kotlin
dependencies {
    compileOnly("com.github.chasehuegel:skilling:main-SNAPSHOT")
}
```

## Accessing the API

```java
SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
Registries registries = api.getRegistries();
```

## Registering a Custom Mechanic

Mechanics implement `SkillMechanic`. Each implementation declares its YAML key,
its purpose, and its supported parameters. Give every mechanic a class-level Javadoc
describing its YAML key, purpose, and required/optional parameters.

```java
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Map;

/**
 * Applies a directional velocity impulse (knockback) to the damaged entity.
 *
 * <p>YAML key: {@code myaddon:knockback}
 * <br>Params:
 * <ul>
 *   <li>{@code force} (double). Impulse strength, must be &gt; 0 to act</li>
 *   <li>{@code vertical} (double, optional, default 0.3). Upward component</li>
 * </ul>
 */
public class KnockbackMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double force = ((Number) params.getOrDefault("force", 0.0)).doubleValue();
        if (force <= 0) return false;
        double vertical = ((Number) params.getOrDefault("vertical", 0.3)).doubleValue();
        if (event instanceof EntityDamageByEntityEvent de
                && de.getEntity() instanceof LivingEntity target) {
            target.setVelocity(player.getLocation().getDirection().multiply(force)
                    .setY(vertical));
            return true;
        }
        return false;
    }
}
```

Register it in your plugin's `onEnable()`. Passing the parameter names lets the
engine validate ability YAMLs and drives the live registry:

```java
SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
api.getRegistries().getMechanicRegistry().register(
        "myaddon:knockback", KnockbackMechanic.class, List.of("force", "vertical"));
```

Server owners can now use `type: "myaddon:knockback"` in their YAML.

If your mechanic consumes a string-valued parameter (e.g. an effect or material
key), you can register an optional **load-time validator** so a YAML typo is
rejected while the skill file is parsed instead of throwing inside an event
handler. The validator receives the mechanic's constant-valued parameters and
throws an `IllegalArgumentException` for an invalid value:

```java
registry.register("myaddon:knockback", KnockbackMechanic.class, List.of("force", "vertical"),
        (context, params) -> {
            if (params.get("material") != null
                    && org.bukkit.Material.matchMaterial(String.valueOf(params.get("material"))) == null) {
                throw new IllegalArgumentException(context + ": unknown material " + params.get("material"));
            }
        });
```

> **Namespaced effect/attribute keys:** Addon mechanics that resolve potion effects
> through `PotionEffectResolver.resolve(...)` (or attributes through the same registry
> pattern) automatically gain namespaced-key support (`minecraft:poison`) with legacy
> numeric IDs still accepted but deprecated. Unknown keys fail fast.

### Persistent Unlock Mechanics

A mechanic that grants permanent player state and is intrinsically idempotent
(for example unlocking a recipe, a quest, or a persistent trait) should
implement `UnlockMechanic` instead of the plain `SkillMechanic` marker. The
engine treats unlock mechanics as one-time milestones: it re-runs them on
player join and after `/skills reload` for every ability whose owning-skill
level already meets `unlock_level`, and the `level_up` trigger fires them at
the milestone moment in-session.

Two contracts follow from reconciliation:

- **The mechanic must be idempotent.** Return `false` once the state already
  exists, so a re-run (later level-up, join, reload) is a no-op and never
  re-fires feedback.
- **The `event` argument may be `null`.** Join and reload reconciliation calls
  `execute(player, params, null)`. Guard any event-specific logic.

```java
import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;

/**
 * Permanently unlocks a quest entry for the player.
 *
 * <p>YAML key: {@code myaddon:unlock_quest}
 * <br>Params: {@code quest} (namespaced quest id)
 */
public class UnlockQuestMechanic implements UnlockMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        String quest = String.valueOf(params.get("quest"));
        if (player.getPersistentDataContainer().has(questKey(quest),
                org.bukkit.persistence.PersistentDataType.BYTE)) {
            return false; // already unlocked
        }
        // grant the quest, store the flag persistently, then:
        return true;
    }
}
```

Register it as usual; the registry reports it through `isUnlock(...)` and the
engine's reconciliation path picks it up automatically.

### Proc-Aware Mechanics and Success-Gated Feedback

A mechanic whose effect is probabilistic (a dodge, block, or cancel that
succeeds only when a chance roll wins) can implement `ProcAwareMechanic` so the
engine can gate an ability's `feedback.success_only` cues to the roll actually
landing. Without this, feedback fires on every execution attempt — a player
would get a "Dodged!" cue even on a missed roll, making the ability feel noisy.

Implement the extra `didProc()` method to report whether the most recent
`execute` actually produced its effect. Mechanics are prototype-scoped
(recreated per dispatch), so storing the roll outcome on the instance is
thread-safe.

```java
import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;

/**
 * Rolls a chance to negate a player's damage.
 *
 * <p>YAML key: {@code myaddon:parry}
 * <br>Params: {@code chance} (0-100)
 */
public class ParryMechanic implements ProcAwareMechanic {

    private boolean procced;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof org.bukkit.event.entity.EntityDamageEvent de)
                || !de.getEntity().equals(player)) {
            return false;
        }
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        procced = ThreadLocalRandom.current().nextDouble(100) <= chance;
        if (procced) de.setCancelled(true);
        return true; // an activation attempt regardless of roll outcome
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}
```

An ability using such a mechanic can then set `feedback.success_only: true` so
its action bar, sounds, and particles fire only when the parry actually lands.

## Registering a Custom Trigger

Triggers map a YAML trigger key to a Paper event class. Implement `SkillTrigger` as a
`record` that returns its key and event class:

```java
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityResurrectEvent;

/**
 * Trigger fired when a player is resurrected by a Totem of Undying.
 *
 * <p>YAML key: {@code resurrect}
 */
public record ResurrectTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "resurrect"; }

    @Override
    public Class<? extends Event> getEventClass() { return EntityResurrectEvent.class; }
}
```

Register it in your plugin's `onEnable()`:

```java
SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
api.getRegistries().getTriggerRegistry().register("resurrect", ResurrectTrigger.class);
```

### Adding the Event Listener

Registering the trigger only binds the key to an event class. The engine still needs
an `@EventHandler` to observe the event and call `dispatch()`. Add a listener mirroring
the engine's own pattern (`SkillEventListener.onResurrect`):

```java
import io.github.chasehuegel.skilling.Skilling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityResurrectEvent;

public class ResurrectListener implements org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            Skilling.getInstance().getSkillEventListener().dispatch(player, event, "resurrect");
        }
    }
}
```

Register the listener with Bukkit (`getServer().getPluginManager().registerEvents(...)`)
in your `onEnable()`. Server owners can then use `trigger: "resurrect"` on their abilities.

## Registering a Custom State Filter
State filters are evaluated against a player, the triggering event, and the filter
value after the `:` in YAML (e.g., `state: "equipped_all:#c:heavy_armor"`). Register them as lambdas
on the `StateFilterRegistry`:

```java
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;

StateFilterRegistry sf = Skilling.getInstance().getStateFilterRegistry();
sf.register("equipped", (p, e, v) -> {

    var armor = p.getInventory().getArmorContents();
    return switch (v) {
        case "heavy" -> {
            boolean allHeavy = true;
            for (var piece : armor) {
                var t = piece.getType();
                if (!t.name().startsWith("DIAMOND_") && !t.name().startsWith("NETHERITE_")) {
                    allHeavy = false;
                    break;
                }
            }
            yield allHeavy;
        }
        default -> false;
    };
});
```

Armor gating is provided by the built-in `equipped_all` / `equipped_any` filters, which
resolve their `value` as a material or `#...` tag through the cached `TagResolver`. No
armor-tier knowledge lives in Java. The bundled `#c:light_armor`, `#c:medium_armor`,
`#c:heavy_armor`, and `#c:unarmored` tags reproduce the historical tiers as data (see
`tags/base.yml`). Custom filters accept any `value`
string and are available in both `requirements.state` and filter `state` fields.

### Constructor Requirements

Both `SkillMechanic` and `SkillTrigger` implementations **must** have a public no-argument constructor. The registries use `Class::newInstance()` to instantiate them at runtime. This is **validated at registration time** (fail-fast). A class without a public no-arg constructor is rejected the moment you call `register(...)`, not on first use. `ParameterEvaluator` implementations can be registered either as instances (no constructor constraint) or as classes. Classes must have a public no-arg constructor.

## Ability Schema: The `trigger` Field

Every ability **must** declare exactly one `trigger` key that binds it to a single event
dispatch (e.g., `block_break`, `entity_damage_taken`, `player_interact`). This is a
**required** field. Omitting it throws `IllegalArgumentException` during skill loading
(fail-fast), so custom abilities shipped with your addon must always set it:

```yaml
abilities:
  - id: "my_ability"
    display_name: "My Ability"
    unlock_level: 1
    trigger: "player_interact"      # Required; must match a registered trigger key
    mechanics:
      - type: "myaddon:knockback"
        parameters:
          force: { constant: 2.0 }
```

The trigger key must correspond to a registered `SkillTrigger`, and it must be
compatible with the mechanic's expected event (a mechanic that guards on
`EntityDamageByEntityEvent` will not act on a `player_interact` trigger). See
[creating-skills.md](creating-skills.md) for the full schema and
[capabilities.md](capabilities.md) for each mechanic's event.

## Cooldown Evaluators

The `requirements.cooldown` field accepts full evaluator syntax in addition to a plain
scalar, enabling inverse-cooldown sub-scaling as the player levels up:

```yaml
requirements:
  cooldown:
    linear: { base: 5.0, step: -0.02, max: 1.0 }
```

A scalar (e.g., `5.0`) is equivalent to `constant: 5.0`. Use `linear` with a negative
`step` to shrink the cooldown as the player gains levels. The engine evaluates the
cooldown evaluator against `(level, unlockLevel)` before the cooldown check.

## `block_damage` vs `cancel_damage`

Both built-in mechanics are functionally identical: they roll a `chance` (0-100%) to
negate an incoming damage event. They are kept as separate keys purely for flavor.
`core:block_damage` reads as a shield/armor block (used by armor and shield skills)
while `core:cancel_damage` reads as a dodge/evade (used by evasion skills). Do not
merge them in your skill packs. Pick the key that matches the ability's flavor.

## Registering a Custom Evaluator

```java
public class LogisticEvaluator implements ParameterEvaluator {
    private final double midpoint;
    private final double steepness;

    public LogisticEvaluator(double midpoint, double steepness) {
        this.midpoint = midpoint;
        this.steepness = steepness;
    }

    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        double x = currentLevel - unlockLevel;
        return 1.0 / (1.0 + Math.exp(-steepness * (x - midpoint)));
    }
}

SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
api.getRegistries().registerEvaluator("logistic", LogisticEvaluator.class);
```

> Evaluators can be registered either as instances (`registerEvaluator(key, instance)`) or as classes (`registerEvaluator(key, class)`). The class variant is preferred as it enables fresh instantiation per evaluation context.

## PlaceholderAPI

Skilling registers a `%skilling_*%` placeholder expansion when PlaceholderAPI is detected:

| Placeholder | Description | Example Output |
|---|---|---|
| `%skilling_level_{skill}%` | Player's level in a skill | `47` |
| `%skilling_xp_{skill}%` | Raw XP in a skill | `12840` |
| `%skilling_max_xp_{skill}%` | XP needed for next level | `15000` |
| `%skilling_progress_{skill}%` | Percent to next level | `62.5` |
| `%skilling_remaining_{skill}%` | XP remaining for next level | `2160` |
| `%skilling_total_levels%` | Sum of all skill levels | `312` |

Example: `%skilling_level_mining%` returns the player's current Mining level.

### Evaluator Placeholders

Expose dynamic ability parameters for display in scoreboards and chat:

| Placeholder | Description | Example Output |
|---|---|---|
| `%skilling_evaluator_{skill}_{ability}_{param}%` | Dynamic evaluator output | `34.50` |

Example: `%skilling_evaluator_mining_geologist_yield_chance%` returns the current yield chance for Geologist.

## API Reference

### SkillingAPI

| Method | Returns | Description |
|---|---|---|
| `getRegistries()` | `Registries` | Access to all three registries |
| `getProfileManager()` | `ProfileManager` | Player profile cache |
| `getSkillManager()` | `SkillManager` | Loaded skill definitions |
| `getSkillMenuBuilder()` | `SkillMenuBuilder` | GUI inventory builder |
| `getRequirementEngine()` | `RequirementEngine` | Check/consume pipeline |
| `getFeedbackDebouncer()` | `FeedbackDebouncer` | Spam throttle |
| `getBossBarPool()` | `BossBarPool` | LRU Boss Bar cache |
| `getProfile(UUID)` | `PlayerProfileView` | Read-only view of a player's profile (XP queries only). Synchronous in-memory cache lookup, returns null if not loaded. The mutable engine profile is never exposed to addons. |

### Registries

| Method | Description |
|---|---|
| `registerMechanic(String, Class<? extends SkillMechanic>)` | Register a SkillMechanic implementation. Public no-arg constructor validated at registration |
| `registerTrigger(String, Class<? extends SkillTrigger>)` | Register a SkillTrigger implementation. Public no-arg constructor validated at registration |
| `registerEvaluator(String, Class<? extends ParameterEvaluator>)` | Register a ParameterEvaluator implementation by class |
| `registerEvaluator(String, Object)` | Register a ParameterEvaluator instance (deprecated) |
| `getMechanicRegistry()` | Direct access to mechanic registry |
| `getTriggerRegistry()` | Direct access to trigger registry |
| `getEvaluatorRegistry()` | Direct access to evaluator registry |

All registries return **immutable snapshots** from read methods (`keys()`, `getAllParameterNames()`) and **copy** caller-supplied parameter-name lists defensively. Returned collections and later caller mutation can never corrupt registry state. Duplicate keys and classes lacking a public no-arg constructor fail fast with `IllegalArgumentException`.
