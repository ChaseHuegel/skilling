# ISSUE-279: Shaded JAR omits the SQLite JDBC driver and ships un-remapped service files

## Context & User Story
- **Goal:** As a server owner, I want the plugin to open its SQLite database so Skilling actually runs. The built `skilling-1.0-SNAPSHOT-all.jar` contains zero `org/sqlite` driver classes, so `DatabaseManager` throws `No suitable driver found for jdbc:sqlite:` on `onEnable()` and Bukkit disables the plugin.
- **Agent Role:** You are an expert backend/build engineer executing this task.

## Implementation Requirements
- [x] Make the SQLite JDBC driver classes survive shading. Verify by inspecting the `shadowJar` output: the relocated `libs/sqlite/JDBC.class` (or equivalent driver entry point) must exist.
- [x] Ensure `META-INF/services/java.sql.Driver` in the shaded JAR resolves. Either remap its contents to the relocated driver class name (Shadow service-file transformer) or avoid stripping/relocating in a way that breaks ServiceLoader discovery.
- [x] Audit the remaining relocated `META-INF/services` entries (cloud, Jetty) and fix or document each one.
- [x] Add a regression check so a stripped driver cannot ship again (e.g., assert the driver class exists in the JAR after `shadowJar`).

## Technical Specifications & Context
- **Target Files:** `build.gradle.kts:96-102` (`relocate` blocks for `org.sqlite`, `org.incendo.cloud`, `org.eclipse.jetty`; `minimize()` at line 102). `src/main/java/io/github/chasehuegel/skilling/engine/db/DatabaseManager.java:23-35` (Hikari config uses a bare `jdbc:sqlite:` URL; no `Class.forName`).
- **Dependencies:** `org.xerial:sqlite-jdbc:3.49.1.0` (line 23), `com.zaxxer:HikariCP:6.3.0` (line 22).
- **Constraints:** Keep the relocation to the `io.github.chasehuegel.skilling.libs.*` namespace. `minimize()` strips classes reachable only reflectively or via ServiceLoader. Prefer an explicit `keep(...)` for the driver over disabling `minimize()` globally.

## Verification & Definition of Done
- [x] `unzip -l build/libs/skilling-1.0-SNAPSHOT-all.jar` shows the relocated driver class and a correct `META-INF/services/java.sql.Driver`.
- [x] The regression check fails on the current artifact and passes after the fix.
- [x] `./gradlew build` passes.
- [x] Edge case handled: the plugin boots far enough to open/create `data.db` (verified with a local `runServer` or a direct Hikari-init smoke test).
