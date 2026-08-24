# Report: XP Source Design

Status: Reference design note for tanking and resource XP sources.

This report defines how bundled skills earn XP so the sources stay meaningful,
grindable, and honest. It sets the per-action reward ceilings and the sanctioned
exceptions. Skill YAML under `src/main/resources/skills/` cross-references this
report.

## 1. Purpose

A skill needs a grindable XP source. A good source has an opportunity, time, or
resource cost. A bad source is free and spammable. Use the cost of an action to
set its reward.

## 2. The ~60 XP Per Action Ceiling

Most actions pay about 60 XP or less per action. This keeps a player moving
between resource loops and choices instead of standing in one spot. The XP needed
to level grows with the polynomial curve, so a modest per-action return still
translates into a long, steady grind.

## 3. Sanctioned Outlier: Tanking

Tanking skills (blocking, armor soaking) exceed the ~60 XP per action ceiling on
purpose. Blocking and soaking cost concentration and lock the player out of
attacking the same moment. A mob blow that a shield turns into durability damage
is the direct evidence of a tanking loop. Rewarding it more than the ceiling
reflects that opportunity cost.

Two shields XP sources use this outlier:

- Blocking an incoming hit: `entity_damage_taken` with the `is_blocking` filter,
  scaled by damage.
- Ending a fight with a shield raised: `entity_kill` with the `is_blocking`
  filter.

These pay above the ceiling, then a durability and craft source round out the
loop.

## 4. Material and Durability Sources

Durability wear is a meaningful tank grind because the cost is repairs and the
time combat demands. Use `item_damage` gated on the item tag (for example
`#c:shields`) so a nearly-exhausted defensive item keeps driving the loop. For
shields this source stays minor relative to the tanking outlier: the same block
that wears the shield already paid from the blocking source.

## 5. Craft Sources

Crafting should pay more for a costly, single-output recipe and less for a bulk,
material-cheap one. A shield is 6 planks and 1 iron for a single output, so it
pays a moderate flat return. Crafting sticks or arrows, which is bulk and tea, pays
little.

## 6. Tension With L1 Endurance

If a low-level passive makes gear last longer, an `item_damage` XP source would
dry up as the player levels. Keep the endurance and durability-growth effects
minor, or keep `item_damage` a secondary source so its drying is mild. The shields
skill values the low-level `block_recovery` scalar as endurance without stripping
the durability source.