package io.github.chasehuegel.skilling.engine.lockdown;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.api.Registries;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LockdownManagerReloadTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadThatThrowsNeverLeavesPluginReloading() {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getRegistries()).thenReturn(new Registries(
                new MechanicRegistry(), new TriggerRegistry(), new EvaluatorRegistry()));
        when(plugin.getRequirementEngine()).thenReturn(mock(RequirementEngine.class));
        when(plugin.getSkillEventListener()).thenReturn(mock(io.github.chasehuegel.skilling.engine.listener.SkillEventListener.class));

        SkillMenuBuilder builder = mock(SkillMenuBuilder.class);
        doThrow(new RuntimeException("boom")).when(builder).setGuiLayoutConfig(any());
        when(plugin.getSkillMenuBuilder()).thenReturn(builder);

        AsyncBatchWorker worker = mock(AsyncBatchWorker.class);
        when(worker.flushDirtyProfilesAsync())
                .thenReturn(CompletableFuture.completedFuture(null));

        LockdownManager lockdown = new LockdownManager(plugin,
                mock(ProfileManager.class), worker, mock(SkillManager.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getOnlinePlayers()).thenReturn(java.util.List.of());

            // Phase 5 throws; the finally must still unlock.
            assertThrows(RuntimeException.class, lockdown::reload);
        }

        verify(plugin).setReloading(false);
    }
}
