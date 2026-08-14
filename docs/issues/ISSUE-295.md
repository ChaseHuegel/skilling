# ISSUE-295: Plugin version is hardcoded to `1.0-SNAPSHOT`; tagged releases never report the tag version

## Context & User Story
- **Goal:** As a maintainer, I want the artifact and `paper-plugin.yml` to report the release version from the git tag. `gradle.properties` pins `version=1.0-SNAPSHOT`; `release.yml` extracts the tag into a `VERSION` variable but never passes it to Gradle, so every release ships `skilling-1.0-SNAPSHOT.jar` reporting `1.0-SNAPSHOT` to bStats and `/version` checks.
- **Agent Role:** You are an expert build/CI engineer executing this task.

## Implementation Requirements
- [x] Make Gradle derive the version from the release tag (e.g., `-Pversion=${VERSION}` in the release job, or `git describe` in the build).
- [x] Verify the expanded `paper-plugin.yml` in the built artifact matches the tag.
- [x] Keep the local dev version at `1.0-SNAPSHOT`.

## Technical Specifications & Context
- **Target Files:** `gradle.properties:2` (`version=1.0-SNAPSHOT`), `.github/workflows/release.yml:42-44` (tag extracted into `VERSION`, never passed to Gradle), `build.gradle.kts:112-116` (`processResources` expands `${version}` into `paper-plugin.yml`).
- **Dependencies:** None.
- **Constraints:** Do not break the local `./gradlew build` / `runServer` dev loop.

## Verification & Definition of Done
- [x] A dry run of the release job produces an artifact whose `paper-plugin.yml` version equals the tag.
- [x] Local build still reports `1.0-SNAPSHOT`.
- [x] `./gradlew build` passes.
