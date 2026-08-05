package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.branding.BrandingConfig;
import io.github.chasehuegel.skilling.engine.ui.branding.TemplateRenderer;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

/**
 * Provides a craftable "Skills Guide" book that opens the skill overview menu on right-click.
 *
 * <p>The book is an {@link Material#ENCHANTED_BOOK} with custom model data and a
 * {@link GuideBookTag} so the poison-pill vaporization net never destroys it.
 * A shapeless recipe (book + coal) is registered and auto-unlocked for all
 * players. The item is not consumed on use.
 *
 * <p>Can be disabled via {@code skills_guide_book.enabled} in {@code config.yml}.
 */
public final class SkillsGuideBook implements Listener {

    private static final NamespacedKey RECIPE_KEY = NamespacedKey.fromString("skilling:skills_guide_recipe");
    private static final int CUSTOM_MODEL_DATA = 2001;

    private final Skilling plugin;
    private final ProfileManager profileManager;
    private final SkillMenuBuilder skillMenuBuilder;
    private volatile boolean enabled;
    private boolean listenerRegistered;
    private boolean recipeRegistered;

    public SkillsGuideBook(Skilling plugin, ProfileManager profileManager, SkillMenuBuilder skillMenuBuilder) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.skillMenuBuilder = skillMenuBuilder;
        this.enabled = plugin.getConfig().getBoolean("skills_guide_book.enabled", true);
    }

    /**
     * Updates the enabled state at runtime (from config). Disabling removes the
     * recipe from the server so the book actually leaves the game; re-enabling
     * re-registers it and re-discovers it for online players.
     *
     * @param enabled whether the guide book should be active
     */
    public void setEnabled(boolean enabled) {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        if (enabled && !wasEnabled) {
            register();
        } else if (!enabled && wasEnabled) {
            removeRecipe();
        }
    }

    /**
     * Registers the listener and recipe (idempotent). Called at plugin enable and
     * whenever the book transitions from disabled to enabled at runtime.
     */
    public void register() {
        if (!enabled) return;
        if (!listenerRegistered) {
            listenerRegistered = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
        if (!recipeRegistered) {
            registerRecipe();
            recipeRegistered = true;
            Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipe(RECIPE_KEY));
        }
    }

    private void registerRecipe() {
        ItemStack book = create();
        ShapelessRecipe recipe = new ShapelessRecipe(RECIPE_KEY, book);
        recipe.addIngredient(Material.BOOK);
        recipe.addIngredient(Material.COAL);
        Bukkit.addRecipe(recipe, false);
    }

    /**
     * Removes the recipe from the server. Called when the book is disabled at
     * runtime and on plugin disable; Bukkit does not remove recipes automatically.
     */
    public void shutdown() {
        removeRecipe();
    }

    private void removeRecipe() {
        if (recipeRegistered) {
            Bukkit.removeRecipe(RECIPE_KEY);
            recipeRegistered = false;
        }
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.editMeta(meta -> {
            BrandingConfig branding = Skilling.getInstance() != null
                    ? Skilling.getInstance().getBranding()
                    : BrandingConfig.DEFAULT;
            meta.displayName(TemplateRenderer.toComponent(
                    TemplateRenderer.renderLine(branding.guideBook().name(), Map.of())));
            meta.lore(List.of(
                    TemplateRenderer.toComponent(
                            TemplateRenderer.renderLine(branding.guideBook().lore(), Map.of()))
            ));
            meta.setCustomModelData(CUSTOM_MODEL_DATA);
            GuideBookTag.apply(meta);
        });
        return book;
    }

    @EventHandler
    public void onGuideBookInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return;
        if (!GuideBookTag.isTagged(item.getItemMeta())) return;
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
