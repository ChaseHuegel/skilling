package io.github.chasehuegel.skilling.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import io.github.chasehuegel.skilling.engine.ui.SkillsGuideBook;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillsGuideBookTest {

    private static final NamespacedKey RECIPE_KEY = NamespacedKey.fromString("skilling:skills_guide_recipe");

    private SkillsGuideBook bookWithConfig(boolean enabled) {
        var plugin = mock(Skilling.class);
        var config = new YamlConfiguration();
        config.set("skills_guide_book.enabled", enabled);
        when(plugin.getConfig()).thenReturn(config);
        return new SkillsGuideBook(plugin, mock(ProfileManager.class), mock(SkillMenuBuilder.class));
    }

    @Test
    void setEnabledFalseDisablesInteractionsAtRuntime() {
        var plugin = mock(Skilling.class);
        var config = new YamlConfiguration();
        config.set("skills_guide_book.enabled", true);
        when(plugin.getConfig()).thenReturn(config);

        var book = new SkillsGuideBook(plugin, mock(ProfileManager.class), mock(SkillMenuBuilder.class));
        book.setEnabled(false);

        var player = mock(Player.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);

        book.onGuideBookInteract(event);

        verify(player, never()).openInventory(any(org.bukkit.inventory.Inventory.class));
    }

    @Test
    void disableRemovesRecipeAndReEnableRestoresIt() {
        var book = bookWithConfig(true);
        ItemStack bookItem = mock(ItemStack.class);
        try (MockedConstruction<ShapelessRecipe> recipes = mockConstruction(ShapelessRecipe.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SkillsGuideBook> bookStatic = mockStatic(SkillsGuideBook.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            when(Bukkit.getOnlinePlayers()).thenReturn(Set.of());
            bookStatic.when(SkillsGuideBook::create).thenReturn(bookItem);

            book.register();
            bukkit.verify(() -> Bukkit.addRecipe(any(ShapelessRecipe.class), eq(false)));

            book.setEnabled(false);
            bukkit.verify(() -> Bukkit.removeRecipe(RECIPE_KEY));

            book.setEnabled(true);
            bukkit.verify(() -> Bukkit.addRecipe(any(ShapelessRecipe.class), eq(false)), times(2));
        }
    }

    @Test
    void shutdownRemovesRecipeOnDisable() {
        var book = bookWithConfig(true);
        ItemStack bookItem = mock(ItemStack.class);
        try (MockedConstruction<ShapelessRecipe> recipes = mockConstruction(ShapelessRecipe.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SkillsGuideBook> bookStatic = mockStatic(SkillsGuideBook.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            when(Bukkit.getOnlinePlayers()).thenReturn(Set.of());
            bookStatic.when(SkillsGuideBook::create).thenReturn(bookItem);

            book.register();
            book.shutdown();
            bukkit.verify(() -> Bukkit.removeRecipe(RECIPE_KEY));
        }
    }
}
