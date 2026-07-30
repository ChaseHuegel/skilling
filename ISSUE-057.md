# Gradle maven-publish Configuration

## Issue

The project has **no Gradle publishing configuration**. The `settings.gradle.kts` contains only `rootProject.name = "skilling"` and `build.gradle.kts` has no `maven-publish` plugin. This means:

- Addon developers cannot depend on Skilling's API via standard build tooling
- The `SkillingAPI` class and `Registries` container exist but are only accessible by dropping the full plugin JAR as a compile-time dependency
- No published Maven coordinates exist for the project (no JitPack, no Maven Central, no Hangar artifact repo)
- Other Gradle projects must use flat-file `implementation(files(...))` to depend on Skilling

## Scope

Configure Gradle `maven-publish` to publish a **separate API artifact** (see ISSUE-060 for the module split) to Maven repositories. For the immediate term, configure for **JitPack** (zero-registration publishing via GitHub). Optionally configure for **Maven Central** (requires Sonatype account and GPG signing).

| Target | Effort | Visibility | Best For |
|--------|--------|-----------|----------|
| JitPack | Low (add config) | Public | Quick availability, no account needed |
| Maven Central | High (account, GPG, namespace verification) | Public | Permanent authoritative home |

**Recommendation:** Start with JitPack now, add Maven Central as a follow-up.

## Affected Files

| File | Action |
|------|--------|
| `build.gradle.kts` | Add `maven-publish` plugin and publishing block |
| `settings.gradle.kts` | No change needed (JitPack infers group/version from build script) |
| `gradle.properties` | Ensure `group` and `version` properties are set |

## Development Plan

### Step 1: Add `maven-publish` plugin

In `build.gradle.kts`, add the plugin:

```kotlin
plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta10"
    id("maven-publish")
}
```

### Step 2: Configure the publication

Add a publishing block that creates a MavenPublication for the API jar. Since the API module doesn't exist yet (ISSUE-060), publish the **entire plugin JAR** as a stopgap:

```kotlin
publishing {
    publications {
        create<MavenPublication>("skilling") {
            from(components["java"])
            artifactId = "skilling"
            version = project.version as String

            pom {
                name = "Skilling"
                description = "A flexible, data-driven RPG skills engine for PaperMC"
                url = "https://github.com/chasehuegel/skilling"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "chasehuegel"
                        name = "ChaseHuegel"
                        email = "chase@bitfish.dev"  // Use actual email
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/chasehuegel/skilling.git"
                    developerConnection = "scm:git:ssh://github.com/chasehuegel/skilling.git"
                    url = "https://github.com/chasehuegel/skilling"
                }
            }
        }
    }

    repositories {
        maven {
            name = "Local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
```

This creates a publication that:
- Uses the `java` component (all compiled classes)
- Generates standard Maven POM with project metadata
- Publishes to a local `build/repo/` directory for testing

### Step 3: Configure JitPack compatibility

JitPack reads project metadata from Gradle directly. Two requirements:

1. **Ensure `group` is set in `gradle.properties`** (JitPack uses it for the Maven groupId):

```properties
group=io.github.chasehuegel
version=1.0-SNAPSHOT
```

2. **Ensure the build resolves correctly on JitPack's server** — JitPack runs `./gradlew build` and `./gradlew publishToMavenLocal` automatically. Verify:
   - All dependencies are resolvable from public repos
   - No system-specific paths or environment variables
   - The `run-paper` plugin doesn't interfere (it's only used for local dev)

### Step 4: Test the publication locally

```bash
# Publish to local directory
./gradlew publishSkillingPublicationToLocalRepository

# Verify the POM and JAR exist
ls build/repo/io/github/chasehuegel/skilling/
```

### Step 5: Verify JitPack resolves the artifact

1. Push a commit with the publishing config to GitHub
2. Visit `https://jitpack.io/#chasehuegel/skilling`
3. Click "Get it" — JitPack clones the repo, runs the build, and hosts the artifact
4. Note the resolved Maven coordinates:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.chasehuegel</groupId>
    <artifactId>skilling</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```

### Step 6: Update `docs/api-integration.md` with Maven coordinates

Once JitPack resolves, update the documentation to show the correct coordinates:

```xml
<!-- Gradle (build.gradle.kts) -->
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.chasehuegel:skilling:main-SNAPSHOT")
}

<!-- Maven (pom.xml) -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.chasehuegel</groupId>
        <artifactId>skilling</artifactId>
        <version>main-SNAPSHOT</version>
    </dependency>
</dependencies>
```

Note: Once ISSUE-060 (separate `skilling-api` module) is done, the coordinates will change to the API module artifact.

### Step 7 (Optional): Maven Central Preparation

If publishing to Maven Central later:

1. Register a Sonatype account at https://issues.sonatype.org
2. Verify namespace ownership (`io.github.chasehuegel`)
3. Add signing plugin: `id("org.gradle.signing") version "..."` 
4. Add `signing` block to `build.gradle.kts`
5. Configure `sonatype` repository in `publishing.repositories`

This is deliberately excluded from this issue scope — plan it as a separate follow-up.

## Testing

- Run `./gradlew publishSkillingPublicationToLocalRepository` and verify the output JAR and POM exist in `build/repo/`
- Verify the POM contains correct coordinates, description, license, and SCM fields
- Add the local repo as a test repository in a separate dummy Gradle project and verify `compileOnly("io.github.chasehuegel:skilling:1.0-SNAPSHOT")` resolves
- Push to GitHub and verify JitPack resolves at `https://jitpack.io/#chasehuegel/skilling`

## Self-Review

- JitPack is the lowest-effort path to publishing — no accounts, GPG keys, or credential management required
- The publication publishes the full shadow JAR, which includes shaded dependencies. This is not ideal for addon developers (they get HikariCP, SQLite, etc. on their compile classpath). ISSUE-060 (skilling-api module) will fix this by publishing only the API interfaces.
- The `run-paper` plugin is in the `buildscript` classpath but shouldn't interfere with JitPack — it's not used in the default build task
- JitPack picks the latest commit on the default branch unless a specific commit/tag is requested. Tagged releases (`v1.0.0`) create stable artifacts.
- The POM SCM connection URL uses SSH format — this is standard and matches JitPack's expectations

## Future Considerations

- After ISSUE-060, publish the `skilling-api` module separately with `artifactId = "skilling-api"` and a slimmer dependency footprint
- Add Maven Central as a permanent home with proper GPG signing
- Add a `CHANGELOG.md` referenced from the POM via `<scm>` for release notes
- Consider `gradle-maven-publish-plugin` for additional repository formats (GitHub Packages, Hangar)
