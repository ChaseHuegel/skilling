package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

/**
 * Provides a craftable "Skills Guide" book that opens the skill overview menu on right-click.
 *
 * <p>The book is an {@link Material#ENCHANTED_BOOK} with custom model data and a
 * {@link PoisonPillTag} for inventory security. A shapeless recipe (book + coal) is
 * registered and auto-unlocked for all players. The item is not consumed on use.
 *
 * <p>Can be disabled via {@code skills_guide_book.enabled} in {@code config.yml}.
 */
public final class SkillsGuideBook implements Listener {

    private static final NamespacedKey RECIPE_KEY = NamespacedKey.fromString("skilling:skills_guide_recipe");
    private static final int CUSTOM_MODEL_DATA = 2001;

    private final Skilling plugin;
    private final ProfileManager profileManager;
    private final SkillMenuBuilder skillMenuBuilder;
    private final boolean enabled;

    public SkillsGuideBook(Skilling plugin, ProfileManager profileManager, SkillMenuBuilder skillMenuBuilder) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.skillMenuBuilder = skillMenuBuilder;
        this.enabled = plugin.getConfig().getBoolean("skills_guide_book.enabled", true);
    }

    public void register() {
        if (!enabled) return;
        registerRecipe();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipe(RECIPE_KEY));
    }

    private void registerRecipe() {
        ItemStack book = create();
        ShapelessRecipe recipe = new ShapelessRecipe(RECIPE_KEY, book);
        recipe.addIngredient(Material.BOOK);
        recipe.addIngredient(Material.COAL);
        Bukkit.addRecipe(recipe, false);
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.editMeta(meta -> {
            meta.displayName(Component.text("Skills Guide", NamedTextColor.GOLD));
            meta.lore(List.of(
                    Component.text("Right-click to open your skills", NamedTextColor.GRAY)
            ));
            meta.setCustomModelData(CUSTOM_MODEL_DATA);
            PoisonPillTag.apply(meta);
        });
        return book;
    }

    @EventHandler
    public void onGuideBookInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return;
        if (!PoisonPillTag.isTagged(item.getItemMeta())) return;
        event.setCancelled(true);
        var profile = profileManager.getOrCreate(event.getPlayer());
        event.getPlayer().openInventory(skillMenuBuilder.buildOverview(profile));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        event.getPlayer().discoverRecipe(RECIPE_KEY);
    }
}
