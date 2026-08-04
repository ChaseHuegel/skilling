package io.github.chasehuegel.skilling.engine.command;

import io.github.chasehuegel.skilling.Skilling;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code /skills set} never reports success for a config key that only
 * takes effect on restart.
 */
class SkillsCommandConfigSetTest {

    private static String serialize(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    @Test
    void poolSizeChangeSavesConfigAndRequiresRestart() {
        YamlConfiguration config = new YamlConfiguration();
        Skilling plugin = mock(Skilling.class);
        when(plugin.getConfig()).thenReturn(config);
        CommandSender sender = mock(CommandSender.class);

        new SkillsCommand(plugin, null, null, null, null, null)
                .handleSetConfig(sender, "database.pool_size", "20");

        assertEquals(20, config.getInt("database.pool_size"));
        verify(plugin).saveConfig();
        verify(plugin, never()).reloadConfigSettings();
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(captor.capture());
        assertTrue(serialize(captor.getValue()).contains("restart"),
                "a pool-size change must be reported as restart-required, not applied");
    }

    @Test
    void liveApplicableKeyStillAppliesImmediately() {
        YamlConfiguration config = new YamlConfiguration();
        Skilling plugin = mock(Skilling.class);
        when(plugin.getConfig()).thenReturn(config);
        CommandSender sender = mock(CommandSender.class);

        new SkillsCommand(plugin, null, null, null, null, null)
                .handleSetConfig(sender, "bossbar.max_active", "3");

        assertEquals(3, config.getInt("bossbar.max_active"));
        verify(plugin).reloadConfigSettings();
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(captor.capture());
        assertFalse(serialize(captor.getValue()).contains("restart"),
                "a live-applicable key must report success without a restart caveat");
    }

    @Test
    void requiresRestartOnlyForConstructionTimeKeys() {
        assertTrue(SkillsCommand.requiresRestart("database.pool_size"));
        assertFalse(SkillsCommand.requiresRestart("bossbar.max_active"));
        assertFalse(SkillsCommand.requiresRestart("bossbar.fade_ticks"));
        assertFalse(SkillsCommand.requiresRestart("debouncer.interval_ms"));
        assertFalse(SkillsCommand.requiresRestart("skills_guide_book.enabled"));
    }
}
