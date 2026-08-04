# ISSUE-254: Rename `bStatsHook` to follow Java naming conventions

## Context & User Story
- **Goal:** As an engine maintainer, I want type names to follow the standard UpperCamelCase convention.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Nit — `bStatsHook` (`src/main/java/io/github/chasehuegel/skilling/engine/integration/bStatsHook.java`) starts lowercase, and the field/getter in `IntegrationManager` mirror it (`IntegrationManager.java:9`, `:37`).

## Implementation Requirements
- [x] Rename the class to `BStatsHook` (or `BStatsMetrics`), update the field name and `hasbStats()` getter, and update any references.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/integration/bStatsHook.java` (rename)
  - `src/main/java/io/github/chasehuegel/skilling/engine/integration/IntegrationManager.java`
- **Dependencies:** none.
- **Constraints:** None.

## Verification & Definition of Done
- [x] No `bStatsHook` identifiers remain.
- [x] `./gradlew build` and `./gradlew test` pass.
