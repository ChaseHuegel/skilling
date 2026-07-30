# Separate `skilling-api` Module

## Issue

Addon developers currently have **no clean API artifact** to depend on. The full plugin JAR (~5MB with shaded dependencies) must be used as a compile-time dependency, pulling in HikariCP, SQLite, Javalin, Cloud, and Jetty classes — none of which are relevant for addon development. This creates:

- Bloated addon build classpaths
- Risk of class conflicts if addons shade their own versions of relocated dependencies
- Poor developer experience — no clear boundary between "public API" and "internal implementation"
- The `SkillingAPI` service class lives in the same package as implementation details

## Scope

Extract the public API surface into a **separate Gradle subproject** (`skilling-api`) that produces a slim JAR containing only:
- `SkillingAPI.java` — The main API entry point
- `SkillMechanic.java` — Interface for custom mechanics
- `SkillTrigger.java` — Interface for custom triggers
- `ParameterEvaluator.java` — Interface for custom evaluators
- `RequirementResult.java` — Result object for requirement checks
- `FailureReason.java` — Enum for failure types
- `SkillDefinition.java` — Record types for skill data model
- `PlayerProfile.java` — Player state interface (or a read-only view)
- `Registries.java` — Registry container for addon registration

The module should have **zero dependencies** beyond the Paper API (and possibly a minimal subset of existing dependencies).

## Affected Files

| File | Action |
|------|--------|
| `settings.gradle.kts` | Add `include("skilling-api")` |
| `skilling-api/build.gradle.kts` | Create |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/api/` | Create (API interfaces/classes) |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/evaluator/ParameterEvaluator.java` | Move or symlink |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/mechanic/SkillMechanic.java` | Move or symlink |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/trigger/SkillTrigger.java` | Move or symlink |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementResult.java` | Move or symlink |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/api/Registries.java` | Move existing `api/Registries.java` |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/api/SkillingAPI.java` | Move existing `api/SkillingAPI.java` |
| `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/profile/PlayerProfile.java` | Create read-only interface |
| `plugin/build.gradle.kts` | Rename `build.gradle.kts` → `plugin/build.gradle.kts` |
| `plugin/src/` | Move all existing source into `plugin/` |
| `docs/api-integration.md` | Update Maven/Gradle coordinates |
| `docs/getting-started.md` | Add API module note |

## Development Plan

### Step 1: Decide module structure

Two approaches:

| Approach | Structure | Pros | Cons |
|----------|-----------|------|------|
| **A: Multi-module Gradle** | `skilling-api/` + `plugin/` subprojects | Clean separation, standard Gradle pattern | Significant file moves, existing paths change |
| **B: In-source API package** | Publish from existing structure using artifact filter | No file moves, simpler initial setup | Less clean, risk of leaking internals |

**Recommendation: Approach A (Multi-module).** Cleaner long-term, standard in the Minecraft plugin ecosystem.

New directory layout:
```
skilling/
├── settings.gradle.kts          # rootProject.name + include("skilling-api", "plugin")
├── build.gradle.kts             # empty or minimal root config
├── skilling-api/
│   ├── build.gradle.kts         # compileOnly paper-api, no shading
│   └── src/main/java/...        # API interfaces + data classes
├── plugin/
│   ├── build.gradle.kts         # existing build config, depends on :skilling-api
│   └── src/main/java/...        # existing implementation code
└── web/                         # unchanged
```

### Step 2: Create root `build.gradle.kts`

```kotlin
// Root build file — delegates to subprojects
plugins {
    id("java-library") apply false
}
```

### Step 3: Update `settings.gradle.kts`

```kotlin
rootProject.name = "skilling"

include("skilling-api")
include("plugin")
```

### Step 4: Create `skilling-api/build.gradle.kts`

```kotlin
plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withJavadocJar()           // Publish javadoc for API consumers
    withSourcesJar()           // Publish sources for IDE debugging
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Key points:
- **Zero runtime dependencies** — only Paper API at compile time
- No Shadow plugin — no shading needed for API module
- Javadoc and sources jars for IDE support
- Version matches root project version

### Step 5: Identify the API surface

Classes to include in `skilling-api/src/main/java/`:

```
io.github.chasehuegel.skilling.api/
    SkillingAPI.java              # Main API entry (Bukkit Service)
    Registries.java               # Registry container (addon registration)
    SkillDefinition.java          # Public data model records (move from engine/)
        Skill.java                # (or inline)
        Ability.java              # (or inline)

io.github.chasehuegel.skilling.engine.mechanic/
    SkillMechanic.java            # @FunctionalInterface (move)

io.github.chasehuegel.skilling.engine.trigger/
    SkillTrigger.java             # Interface (move)

io.github.chasehuegel.skilling.engine.evaluator/
    ParameterEvaluator.java       # @FunctionalInterface (move)

io.github.chasehuegel.skilling.engine.requirements/
    RequirementResult.java        # Record (move)
    FailureReason.java            # Enum (move)

io.github.chasehuegel.skilling.engine.profile/
    PlayerProfileView.java        # READ-ONLY interface for PlayerProfile
```

**New `PlayerProfileView` interface:**

```java
public interface PlayerProfileView {
    UUID getPlayerId();
    long getXp(String skillId);
    int getLevel(String skillId);
    Map<String, Long> getXpSnapshot();
    boolean isInitialized();
}
```

The existing `PlayerProfile` implements this. The API module only exposes the interface, not the concrete implementation with `isDirty` flags and concurrent maps.

**Important:** `SkillDefinition.java` currently exists in `engine/` package. It should be moved to `api/` package in the API module, and the implementation module references it from the API:

```java
// In plugin module — imports from skilling-api
import io.github.chasehuegel.skilling.api.SkillDefinition;
```

### Step 6: Move plugin source to `plugin/` subdirectory

1. Create `plugin/` directory
2. Move existing source: `src/` → `plugin/src/`
3. Move `build.gradle.kts` → `plugin/build.gradle.kts`
4. Update `plugin/build.gradle.kts`:

```kotlin
plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta10"
}

dependencies {
    implementation(project(":skilling-api"))  // depends on API module
    
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    // ... rest of existing dependencies
}
```

### Step 7: Handle internal classes

Classes that should remain in the `plugin` module (not in API):

- All mechanic implementations (`engine/mechanic/impl/*.java`)
- All trigger implementations (`engine/trigger/impl/*.java`)
- All evaluator implementations (`engine/evaluator/impl/*.java`)
- Database classes (`engine/db/*.java`)
- Registry container (`engine/registry/*.java`)
- Profile manager (`engine/profile/ProfileManager.java`)
- Lockdown manager, feedback, UI, command, tag, web packages

**Registries.java** is a gray area — it's in `api/` package currently. It provides `registerMechanic()`, `registerTrigger()`, `registerEvaluator()` methods that addon developers call. Keep `Registries` in the API module but have it reference registry interfaces that the plugin module implements internally.

### Step 8: Update build tasks

The Shadow JAR for the plugin module should include the API module classes:

```kotlin
// plugin/build.gradle.kts
shadowJar {
    dependsOn(":skilling-api:jar")
    from(project(":skilling-api").tasks.named("jar"))
    // ... existing relocations
}
```

Or rely on `implementation(project(":skilling-api"))` which automatically includes the API classes in the Shadow JAR when shading.

### Step 9: Update publishing

The `maven-publish` configuration (from ISSUE-057) should publish **both** artifacts:

```kotlin
// skilling-api/build.gradle.kts
publishing {
    publications {
        create<MavenPublication>("skillingApi") {
            from(components["java"])
            artifactId = "skilling-api"
            // ... POM metadata
        }
    }
}

// plugin/build.gradle.kts
publishing {
    publications {
        create<MavenPublication>("skillingPlugin") {
            // Publish the shadow JAR
            artifact(tasks.shadowJar)
            artifactId = "skilling"
            // ... POM metadata
        }
    }
}
```

Addon developers depend on:
```kotlin
dependencies {
    compileOnly("com.github.chasehuegel:skilling-api:main-SNAPSHOT")
}
```

### Step 10: Update documentation

**`docs/api-integration.md`:**

```markdown
## Dependency Setup

### Gradle (build.gradle.kts)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.chasehuegel:skilling-api:v1.0.0")
}
```

### Maven (pom.xml)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.chasehuegel</groupId>
        <artifactId>skilling-api</artifactId>
        <version>v1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
```

## Testing

- `./gradlew :skilling-api:build` compiles the API module independently
- `./gradlew :plugin:build` compiles the plugin module with the API as a dependency
- `./gradlew build` at root builds everything
- Verify addon project can compile against `skilling-api` JAR without the plugin JAR
- Verify the shadow JAR includes all API classes
- Verify no Paper API internals are leaked in the API module (compileOnly prevents this)

## Self-Review

- Module split is standard Gradle multi-project — no unusual build complexity
- The API module is intentionally minimal: interfaces, records, and one abstract class
- `PlayerProfileView` interface ensures addons cannot access internal `isDirty`/`putXp()` methods
- Zero runtime dependencies for the API module — addons only need Paper API + skilling-api
- No code duplication: API interfaces are defined once in `skilling-api` and referenced from `plugin`
- The module split is backwards-compatible for internal code: existing package names stay the same, just some classes move to a different JAR
- Server owners still only download one JAR (the shadow JAR includes the API classes)
- Addon developers download the slim `skilling-api` JAR for compilation

## Future Considerations

- **API versioning:** Once the API is published, follow semver strictly. Breaking changes = major version bump
- **API stability annotation:** Add `@SkillingApiStatus.STABLE`, `.EXPERIMENTAL`, `.INTERNAL` annotations to help addon developers understand API maturity
- **API events:** Consider adding `SkillingAbilityActivateEvent`, `SkillingLevelUpEvent` (custom event) to the API module for other plugins to listen to
- **API module as a standalone plugin:** Could the `skilling-api` JAR be loaded as a separate plugin on the server? This would allow addons to softdepend on `skilling-api` and load without the full Skilling plugin (useful for testing)
