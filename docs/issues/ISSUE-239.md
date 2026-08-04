# ISSUE-239: Close custom-tag fail-fast holes (unknown `#c:` refs and scalar tag values pass silently)

## Context & User Story
- **Goal:** As a skill author, I want a typo'd `#c:` tag reference or a malformed tag value rejected at load so filters never silently match nothing.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — two holes let bad custom-tag configs pass load and silently under-match at runtime.

## Implementation Requirements
- [ ] `TagResolver.isKnown` returns `true` for any `#c:` reference whenever the custom tag store is empty (`TagResolver.java:105`). Narrow the deferral so it only applies when `tags.yml`/the loader genuinely could not load (e.g. unit-test/Bukkit-unavailable), and reject an unknown `#c:` key when the store was actually loaded but lacks the key.
- [ ] `CustomTagLoader` silently yields an empty list when a tag value is a scalar instead of a list (`CustomTagLoader.java:70`, `:85`). Reject scalar (and map) tag values with an `IllegalArgumentException`, matching the file's fail-fast behavior for unknown materials/tags.
- [ ] Add tests: unknown `#c:` key rejected when the store is loaded; scalar tag value rejected.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/TagResolver.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/CustomTagLoader.java`
  - `src/test/java/io/github/chasehuegel/skilling/tag/{TagResolverTest,CustomTagLoaderTest}.java`
- **Dependencies:** none.
- **Constraints:** Unit tests run without Bukkit — the "defer" path must stay for that environment (see the `FakeRegistryAccess`/unittest note in `src/AGENTS.md`). Do not break the circular-reference warning behavior documented in `CustomTagLoaderTest.java:98-113` unless intentionally changing it.

## Verification & Definition of Done
- [ ] Unknown `#c:` key in a loaded store fails load.
- [ ] Scalar tag value fails load.
- [ ] Unit-test environment (no Bukkit) still parses.
- [ ] `./gradlew build` and `./gradlew test` pass.
