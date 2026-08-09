# ISSUE-271: ChainBreakMechanic — Optional `target` Parameter (Material or Tag)

## Context & User Story
- **Goal:** As a skill designer, I want `core:chain_break` (and `core:level_break`) to chain to a configurable material or tag, so I can build abilities that break a complex structure (e.g. an ability that fells a tree by breaking the tagged `logs` block and every connected `leaves` block) instead of only chaining to the exact material of the broken origin block.
- **Agent Role:** You are an expert backend engineer executing this task. The `target` parameter is optional and string-typed, matching how other engine tag/material references work (`#minecraft:` tag, `#c:` custom tag, or a single `minecraft:` material). When it is absent, behavior must stay exactly as today: chain to the origin block's own material.

## Implementation Requirements
- [x] Add an optional string parameter `target` to `ChainBreakMechanic`. When present and non-blank, the BFS chain predicate accepts any neighbor whose material is in the resolved target set. When absent or blank, keep the current behavior: chain only to `origin.getType()`.
- [x] Resolve `target` as a material-or-tag through the engine's `TagResolver` (`#minecraft:<tag>`, `#c:<tag>`, or `minecraft:<material>`), flattening the result into an `EnumSet<Material>` for O(1) per-neighbor lookups. Unknown/invalid references must fail fast at load, not at runtime.
- [x] Register the parameter name in the mechanic registry: add `"target"` to the registration list for both `core:chain_break` and `core:level_break` in `Skilling.java` (`mechReg.register(...)` calls near lines 328-329). Without this the load-time unknown-parameter rejection in `SkillManager.parseMechanics` fails every skill using the mechanic.
- [x] Add a load-time validator so a `target` value that is neither a known tag reference nor a known material is rejected when the skill is parsed. Follow the existing `TagResolver.isKnown` / `SkillManager.validateTagReference` pattern; do not use `Material.matchMaterial` alone because it does not accept `#`-prefixed tags.
- [x] Runtime resolution strategy: mechanics are stateless, prototype-scoped, no-arg-constructed instances (`MechanicRegistry.create`) with no `TagResolver` injected today. Choose and implement one sound approach, e.g. resolve the `target` string to an `EnumSet<Material>` once at parse/load time and pass it through the params map, or give the mechanic a static path to the live plugin `TagResolver` (`Skilling.getTagResolver()`). Document the choice in the class Javadoc. Do not resolve tags on every broken block.
- [x] Keep the existing chain-limit clamp (`MAX_CHAIN_LIMIT = 128`), the `PROCESSING`/`isChainProcessing` re-entrancy guard, tool-durability break-out, and protection-plugin `BlockBreakEvent` calls unchanged.
- [x] Update the class-level Javadoc on `ChainBreakMechanic` to list the new optional parameter.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java` (current chain material chosen at lines 88-89; BFS predicate `neighbor.getType() == targetType` at line 110)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (registration lines 328-329)
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/TagResolver.java` (`resolve(String)` returns `EnumSet<Material>`, `isKnown(String)` for fail-fast; material-only parsing via `MechanicParamValidators.material`, lines 121-129, does NOT accept tags)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/MechanicParamValidators.java` (new material-or-tag validator)
  - Tests: `src/test/.../mechanic/impl/ChainBreakMechanicTest.java`, `LevelBreakMechanicTest.java`
  - Docs: `docs/users/capabilities.md` (`### core:chain_break` lines 37-51 and `### core:level_break` lines 53-67), `docs/dev/template-skill.yml`
- **Dependencies:** none. `TagResolver` is already wired into `SkillManager` at load.
- **Constraints:** Per `src/AGENTS.md`, flatten tag resolution into `EnumSet<Material>` at load; mechanics must remain stateless and re-entrant. The default (no `target`) must round-trip identically. Only block materials are in scope (blocks are `Material`s; the entity `EntityTagResolver` is not relevant here). Note `ChainBreakMechanic` is intentionally non-final and subclassed by `LevelBreakMechanic`, which overrides `directions()` — keep the subclass working with the new param.

## Verification & Definition of Done
- [x] `./gradlew build` and `./gradlew test` pass.
- [x] New unit tests cover: no `target` chains to the origin material (existing behavior preserved); `target` as a single material chains only to that material; `target` as a `#minecraft:` / `#c:` tag chains to every member material; an unknown `target` reference is rejected at load with a clear error.
- [x] `core:level_break` honors `target` with its XZ-plane-only expansion.
- [x] `docs/users/capabilities.md` documents the new optional `target` parameter for both mechanics, and `template-skill.yml` shows a tagged-target example (e.g. logs + leaves tree-felling).
