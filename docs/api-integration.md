# API Integration Guide

Skilling exposes a Bukkit `ServicesManager` API for addon plugins to register custom mechanics, triggers, and evaluators.

## Adding Skilling as a Dependency

### Maven

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.chasehuegel</groupId>
    <artifactId>skilling</artifactId>
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
    compileOnly("com.github.chasehuegel:skilling:main-SNAPSHOT")
}
```

## Accessing the API

```java
SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
Registries registries = api.getRegistries();
```

## Registering a Custom Mechanic

```java
public class LifestealMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
        double percentage = ((Number) params.getOrDefault("percentage", 0.1)).doubleValue();
        double heal = damageEvent.getDamage() * percentage;
        player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
        return true;
    }
}

// In your plugin's onEnable():
SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
api.getRegistries().registerMechanic("myaddon:lifesteal", LifestealMechanic.class);
```

Server owners can now use `type: "myaddon:lifesteal"` in their YAML.

## Registering a Custom Trigger

```java
public record MyCustomTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "my_custom_event"; }
    @Override
    public Class<? extends Event> getEventClass() { return MyCustomEvent.class; }
}

SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
api.getRegistries().registerTrigger("my_custom_event", MyCustomTrigger.class);
```

### Constructor Requirements

Both `SkillMechanic` and `SkillTrigger` implementations **must** have a public no-argument constructor. The registries use `Class::newInstance()` to instantiate them at runtime. `ParameterEvaluator` implementations are registered as instances (not classes) and have no such constraint.

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
api.getRegistries().registerEvaluator("logistic", new LogisticEvaluator(10, 0.5));
```

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

## Vault Economy

When Vault is detected, abilities can use economy-based requirements. See `docs/configuration.md` for economy config keys.

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
| `getProfile(UUID)` | `CompletableFuture<PlayerProfile>` | Synchronous in-memory cache lookup wrapped in `CompletableFuture`; does not block the main thread but does not perform async I/O either |

### Registries

| Method | Description |
|---|---|
| `registerMechanic(String, Class<?>)` | Register a SkillMechanic implementation |
| `registerTrigger(String, Class<?>)` | Register a SkillTrigger implementation |
| `registerEvaluator(String, Object)` | Register a ParameterEvaluator instance |
| `getMechanicRegistry()` | Direct access to mechanic registry |
| `getTriggerRegistry()` | Direct access to trigger registry |
| `getEvaluatorRegistry()` | Direct access to evaluator registry |
