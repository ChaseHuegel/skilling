# ISSUE-180: Unresolved lore placeholders spam warnings and render literally in tooltips

## Context & User Story
- **Goal:** As a player, I want ability tooltips to always show real values, never raw `{placeholder}` text, and as a developer I want no `Unresolved lore placeholder` warnings when the skill GUI opens after a fresh reload.
- **Agent Role:** You are an expert backend (Paper) engineer executing this task.

Opening the skill GUI after a fresh reload logs several unresolved-placeholder warnings:

```
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {replant_chance}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {chain_bonus}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {duration}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {duration}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {multiplier_dur}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {kb}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {kb}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {speed}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {jump}
[12:12:17 WARN]: [Skilling] Unresolved lore placeholder: {jump}
```

The unresolved placeholders also stay literally in the tooltip lines, so players see raw tokens instead of formatted values.

## Implementation Requirements
- [ ] Identify every ability lore placeholder that fails to resolve and align it with the actual mechanic parameter key — fix either the YAML lore text or the parameter name.
- [ ] After the fix, opening the GUI after a fresh reload produces zero unresolved-placeholder warnings.
- [ ] Document the placeholder resolution convention: ability lore placeholders resolve against that ability's mechanic parameter keys; only `{level}`, `{max_level}`, `{skill_name}`, `{xp}` are available at the skill level.
- [ ] Consider moving detection to config-load (fail-fast at parse) instead of warning lazily at GUI-open time.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/LoreResolver.java` — warning at line 54; unresolved tokens are kept verbatim (line 56).
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillMenuBuilder.java` — `resolveAbilityLore()` (lines 344-353) builds the evaluator map from `me.parameters()`; `buildSkillLore` resolves skill-level lore against `skillParams` (lines 239-246).
  - Bundled skills under `src/main/resources/skills/**` (audit all abilities).
- **Known mismatches (from the warnings):**
  - `farming.yml` `living_earth` → `{replant_chance}`, `{chain_bonus}` (mechanics expose `yield_chance` only; `core:auto_replant` has no params).
  - `alchemy.yml` `master_alchemist` → `{multiplier_dur}` (two mechanics both expose `multiplier`).
  - `heavy_armor.yml` `juggernaut` → `{kb}` (mechanics expose `amount`).
  - `shields.yml` `indomitable` → `{kb}` (mechanics expose `amount`).
  - `riding.yml` `legendary_rider` → `{speed}` (speed mechanic exposes `multiplier`).
  - `acrobatics.yml` `endurance` and `gravity_defier` → `{jump}` (jump mechanic exposes `multiplier`).
  - Plus any `{duration}` lines whose mechanics lack a `duration` param.
- **Constraints:**
  - Do not regress the dynamic-value feature: placeholders that can resolve must keep injecting live evaluator output.
  - Prefer fixing the YAML lore/params where a real value is intended; a "no such param" warning should be impossible in shipped configs.

## Verification & Definition of Done
- [ ] `./gradlew build` and `./gradlew test` pass
- [ ] Open the skill GUI after a fresh reload → zero `Unresolved lore placeholder` warnings
- [ ] Every tooltip placeholder renders a formatted value (no raw `{...}` tokens)
- [ ] Unit test coverage for `LoreResolver` behavior with unknown placeholders
