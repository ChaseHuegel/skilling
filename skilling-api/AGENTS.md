# AI Agent Instructions for the Skilling API (skilling-api)

This is the published, addon-facing API module. It is the closest DOX contract for all work under `skilling-api/`. Read the root `AGENTS.md` for project-wide rules, then use this file for API-module rules.

## Purpose

Provide the public contract that addon developers implement and consume to extend the engine: interfaces, records, and registries for custom Mechanics, Triggers, and Parameter Evaluators. The engine core (`src/`) depends on and implements this module. It must stay free of runtime dependencies and implementation classes.

## Ownership

- `skilling-api/src/**`: the entire module source tree.
- Published as its own artifact (`skilling-api`), separate from the plugin JAR, with sources and javadoc jars.

## Local Contracts

- **API-First Design:** Expose the entry point to addons via the Bukkit `ServicesManager` (`SkillingAPI`). Addon code registers custom mechanics/triggers/evaluators through the registry container.
- **Interface Contracts:** Ship the contracts (`SkillMechanic`, `SkillTrigger`, `ParameterEvaluator`, registries, `RequirementResult`, `FailureReason`, `PlayerProfileView`, `SkillDefinition`) only. Implementations belong in `src/` and must not leak into this module.
- **Javadoc:** Required on all public API elements. This is the addon-facing surface. Document each interface's purpose and the YAML keys it binds to.
- **Versioning:** Breaking API changes are a MAJOR version bump per `docs/dev/CONVENTIONS-COMMITS.md` (`!` or `BREAKING CHANGE` footer). This is purely a release-labeling rule. Per root `AGENTS.md`, the project is greenfield and there is no backwards-compatibility obligation, so never spend effort keeping the API source-compatible.
- **Dependencies:** `compileOnly` Paper API only. No runtime dependencies, so addons never pull extra transitive deps.
- **Java 21:** Use modern features (Records, Switch Expressions, Pattern Matching).

## Work Guidance

* `docs/users/api-integration.md` documents how addons use this module (Maven/Gradle coordinates, code samples). Keep it in sync with API changes.
* `README.md` contains a short addon example. The authoritative reference is `docs/users/api-integration.md`.

## Verification

* `./gradlew :skilling-api:build` compiles the module and generates javadoc and sources jars.
* Javadoc generation is part of the build (`withJavadocJar()`, `withSourcesJar()`). Any javadoc errors fail it.
* Publishing: `./gradlew :skilling-api:publishToMavenLocal` (see `skilling-api/build.gradle.kts`).

## Child DOX Index

None.
