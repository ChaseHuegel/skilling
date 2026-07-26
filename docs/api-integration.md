# API Integration

Skilling exposes a public API for addon developers to register custom mechanics, triggers, and parameter evaluators.

## Dependency Setup

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("io.github.chasehuegel:skilling:1.0.0")
}
```

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

## Accessing the API

```java
import io.github.chasehuegel.skilling.api.SkillingAPI;

SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
```

## Registering a Custom Mechanic

```java
public class LifestealMechanic implements SkillMechanic {
    private final double percentage;

    public LifestealMechanic(ConfigurationSection config) {
        this.percentage = config.getDouble("percentage");
    }

    @Override
    public void execute(Player player, int currentLevel, int unlockLevel, @Nullable Event event) {
        // Custom logic: heal for a percentage of damage dealt
    }
}

// Register in your plugin's onEnable():
api.getRegistries().registerMechanic("myaddon:lifesteal", LifestealMechanic.class);
```

Server owners can then use `type: "myaddon:lifesteal"` in their skill YAML.

## Registering a Custom Trigger

```java
public class CustomTrigger implements SkillTrigger {
    @Override
    public void register(SkillManager manager) {
        // Register event listeners
    }
}

api.getRegistries().registerTrigger("myaddon:custom_event", CustomTrigger.class);
```

## Registering a Custom Evaluator

```java
public class LogisticEvaluator implements ParameterEvaluator {
    @Override
    public double evaluate(int currentLevel, int unlockLevel) {
        // S-curve growth
        return 100.0 / (1.0 + Math.exp(-0.1 * (currentLevel - unlockLevel)));
    }
}

api.getRegistries().registerEvaluator("logistic", LogisticEvaluator.class);
```

## API Interface

```java
public interface SkillingAPI {
    Registries getRegistries();
    ProfileManager getProfileManager();
    // Query player data
    CompletableFuture<PlayerProfile> getProfile(UUID playerId);
}
```

## Plugin.yml Setup

```yaml
name: MySkillingAddon
version: 1.0.0
main: com.example.MyAddon
depend: [skilling]
api-version: '1.21'
```
