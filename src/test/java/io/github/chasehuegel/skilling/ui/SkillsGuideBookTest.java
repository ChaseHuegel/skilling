package io.github.chasehuegel.skilling.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import io.github.chasehuegel.skilling.engine.ui.SkillsGuideBook;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillsGuideBookTest {

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
}
