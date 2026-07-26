# API Integration Guide

Skilling exposes a Bukkit `ServicesManager` API for addon plugins to register custom mechanics, triggers, and evaluators.

## Adding Skilling as a Dependency

### Maven

```xml
<repository>
    <id>papermc</id>
    <url>https://repo.papermc.io/repository/maven-public/</url>
</repository>

<dependency>
    <groupId>io.github.chasehuegel</groupId>
    <artifactId>skilling</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Gradle

```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.github.chasehuegel:skilling:1.0.0")
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
    public void execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return;
        double percentage = ((Number) params.getOrDefault("percentage", 0.1)).doubleValue();
        double heal = damageEvent.getDamage() * percentage;
        player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
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
| `getProfile(UUID)` | `CompletableFuture<PlayerProfile>` | Player's skill data |

### Registries

| Method | Description |
|---|---|
| `registerMechanic(String, Class<?>)` | Register a SkillMechanic implementation |
| `registerTrigger(String, Class<?>)` | Register a SkillTrigger implementation |
| `registerEvaluator(String, Object)` | Register a ParameterEvaluator instance |
| `getMechanicRegistry()` | Direct access to mechanic registry |
| `getTriggerRegistry()` | Direct access to trigger registry |
| `getEvaluatorRegistry()` | Direct access to evaluator registry |
