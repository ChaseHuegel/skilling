# ISSUE-174: Rework `ProjectileReturnMechanic` — preserve projectile metadata, prevent duplication, support arrows

**Status:** Open
**Type:** Bug
**Severity:** High (latent item-duplication + item-data loss + missing arrow support; the mechanic is currently unreachable until a `projectile_hit` trigger is wired)

---

## Context & User Story

- **Goal:** As a player, I want the projectile-return ability to give me back the exact item I threw — a trident with its enchantments, durability, name, and lore; an arrow with its effects — exactly once, never duplicated, and for arrows to be supported just like tridents.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Preserve the original item's metadata when returning a trident (derive the return item from the projectile's actual item, e.g. `Trident.getItem()`/`AbstractArrow` item accessors) instead of building a fresh `new ItemStack(material)` that drops all enchantments, durability, names, and lore
- [ ] Prevent duplication: for projectiles that persist and can be picked up in vanilla (tridents, arrows), remove the projectile entity so the player receives the item exactly once; keep the fresh-drop behavior only for non-retrievable projectiles (snowballs, eggs)
- [ ] Support arrows: handle `ARROW`, `SPECTRAL_ARROW`, and `TIPPED_ARROW` projectile types, preserving tipped-arrow potion effects via the arrow's item stack
- [ ] Add the missing class-level Javadoc (YAML key `core:projectile_return`, params) per `src/AGENTS.md` §7
- [ ] Add unit tests covering: trident metadata preserved, arrow types returned, projectile entity removed for pickable projectiles (no dupe), snowball/egg fresh-drop unchanged

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ProjectileReturnMechanic.java:12-33` (whole method)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:597-611` (context — the `ProjectileHitEvent` handler applies `ProjectileMechanic` PDC damage and does **not** dispatch a trigger, so this mechanic is currently dead code)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:299` (registration)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ProjectileMechanic.java` (same-event PDC pattern)
- **Dependencies:** Paper API entity accessors (`Trident.getItem()`, `AbstractArrow.getItemStack()`). ISSUE-117 touches the same `ProjectileHitEvent` handler (non-`LivingEntity` hit guard) but is independent.
- **Constraints:** Do not duplicate items; preserve metadata exactly; follow Check-Execute-Consume (`execute` returns true only when an item is actually returned). Note: until a `projectile_hit` trigger is dispatched, this mechanic cannot fire — fixing its logic is the deliverable here; wiring the trigger may be a separate task.

### Root Cause

Three defects in `execute`:

1. **Metadata loss (tridents).** Line 30 builds `new ItemStack(material)` from the projectile's entity type. Enchantments, durability, custom names, and lore on the original trident are discarded.
2. **Duplication.** The projectile entity is never removed. Tridents and arrows persist in the world after a hit and can be picked up naturally, so the player gets the freshly dropped item **and** can still pick up the original — 2 for 1. Only snowballs/eggs vanish on impact, so a fresh drop for those is safe.
3. **Arrows unsupported.** The type switch (lines 23-28) handles only `TRIDENT`/`SNOWBALL`/`EGG`; `ARROW`, `SPECTRAL_ARROW`, and `TIPPED_ARROW` fall through to `null` and return `false`.

### Proposed Fix

- Resolve the return item from the projectile's own item where one exists (`Trident.getItem()`, `AbstractArrow.getItemStack()`) so metadata — including tipped-arrow potion effects — is preserved; fall back to a fresh `ItemStack` only for non-item projectiles.
- For pickable projectiles (tridents, arrows), `projectile.remove()` before giving the item back so it cannot also be picked up; keep the fresh-drop path for snowballs/eggs.
- Add the arrow types to the supported set.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new unit tests
- [ ] Unit test: throwing an enchanted trident returns an item with the same enchantments/metadata
- [ ] Unit test: the returned trident/arrow projectile entity is removed (no pick-up duplication)
- [ ] Unit test: `ARROW`, `SPECTRAL_ARROW`, and `TIPPED_ARROW` are returned; a tipped arrow preserves its potion effects
- [ ] Unit test: snowball/egg still returns a fresh item
- [ ] Class-level Javadoc documents the YAML key and `chance` parameter
