# ISSUE-004: Skills Guide Book — Vanilla+ GUI Access

## Description

Provide an in-game way to open the skills overview UI that feels
vanilla+ instead of requiring the `/skills` command. A crafted
item (Skills Guide book) that opens the menu on right-click, with
an automatically-unlocked recipe so it appears in the vanilla
recipe book.

## Scope

- New item: "Skills Guide" — a crafted book + quill (or written book)
- Custom model data for resource pack support
- Right-click interaction opens the `/skills` menu (same as bare `/skills`)
- Recipe is automatically unlocked for all players on first join
- Recipe is shaped/custom (e.g., book + compass = Skills Guide)
- The item should not be consumed on use
- Must respect the vanilla recipe book unlocking system

## Proposed Solution

### New classes/files

| File | Responsibility |
|------|---------------|
| `engine/ui/SkillsGuideBook.java` | Define item, recipe, interaction listener |
| `resources/recipes/skills_guide.json` | Recipe file (if needed) |

### Implementation

#### 1. Define the Skills Guide item

```java
public final class SkillsGuideBook {
    private static final NamespacedKey ITEM_KEY = 
        NamespacedKey.fromString("skilling:skills_guide");
    private static final NamespacedKey RECIPE_KEY = 
        NamespacedKey.fromString("skilling:skills_guide_recipe");

    public static ItemStack create() {
        ItemStack book = new ItemStack(Material.KNOWLEDGE_BOOK);
        book.editMeta(meta -> {
            meta.displayName(Component.text("Skills Guide", NamedTextColor.GOLD));
            meta.lore(List.of(
                Component.text("Right-click to open your skills", NamedTextColor.GRAY)
            ));
            meta.setCustomModelData(2001);
            PoisonPillTag.apply(meta);
        });
        return book;
    }

    public static void registerRecipe(Skilling plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "skills_guide_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, create());
        recipe.shape(" B ", " C ", "   ");
        recipe.setIngredient('B', Material.BOOK);
        recipe.setIngredient('C', Material.COMPASS);
        Bukkit.addRecipe(recipe);
        // Auto-unlock for all players
        Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipe(key));
    }
}
```

Wait — `KnowledgeBook` opens the recipe book by default. A `WRITABLE_BOOK` or
`WRITTEN_BOOK` is better, or even a `BOOK` with custom model data. The key is
that right-clicking it triggers the skill menu, not the recipe book GUI.

Better: Use `Material.ENCHANTED_BOOK` with custom model data, or `Material.BOOK`.
Register a `PlayerInteractEvent` handler that checks for the PDC tag.

#### 2. Interaction handler

```java
@EventHandler
public void onGuideBookInteract(PlayerInteractEvent event) {
    ItemStack item = event.getItem();
    if (item == null) return;
    if (!PoisonPillTag.isTagged(item.getItemMeta())) return;
    // Open skill menu
    PlayerProfile profile = profileManager.getOrCreate(event.getPlayer());
    event.getPlayer().openInventory(skillMenuBuilder.buildOverview(profile));
    event.setCancelled(true);
}
```

#### 3. Auto-unlock recipe on first join

Add a listener for `PlayerJoinEvent` that calls
`player.discoverRecipe(RECIPE_KEY)` if the player hasn't already
discovered it.

### Steps

1. Create `SkillsGuideBook` class with `create()`, `registerRecipe()`, 
   and interaction listener method.
2. Register the recipe in `Skilling.onEnable()`.
3. Register the event handler in `Skilling.onEnable()` 
   (add to existing listener or new listener).
4. Add `PlayerJoinEvent` handler for auto-unlock.
5. Add config option to enable/disable (default: enabled).
6. Build, test, commit.

## Risk

Low. Self-contained feature with no changes to existing systems.
