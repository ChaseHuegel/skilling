# `ModifyFurnaceOutputMechanic` silently drops overflow items

## Issue

`ModifyFurnaceOutputMechanic` calls `player.getInventory().addItem(drops)` but discards the return value. When the player's inventory is full, `addItem()` returns a `Map` of items that couldn't be added — these items are silently dropped (destroyed).

**ISSUES.md reference:** Line 124

## Root Cause

In `ModifyFurnaceOutputMechanic.java` line 29:
```java
player.getInventory().addItem(drops.iterator().next().asQuantity(bonus));
```

The `addItem()` method returns a `HashMap<Integer, ItemStack>` containing any leftover items that didn't fit. This return value is ignored.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `ModifyFurnaceOutputMechanic.java` | `engine/mechanic/impl/ModifyFurnaceOutputMechanic.java` | 29 |

## Development Plan

### Step 1: Handle overflow gracefully

Replace the discard with a drop-at-feet fallback:

```java
ItemStack bonusItem = drops.iterator().next().asQuantity(bonus);
Map<Integer, ItemStack> leftover = player.getInventory().addItem(bonusItem);
for (ItemStack overflow : leftover.values()) {
    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
}
```

### Step 2: Verify

- Test with full inventory: overflow items should appear on the ground near the player
- Test with empty inventory: items should go directly into the player's inventory

## Self-Review

- Minimal change, no behavioral regression
- `dropItemNaturally` is the standard Minecraft pattern for overflow handling
- Consider using `dropItem` (same location) instead of `dropItemNaturally` (slight random offset) for tighter grouping
