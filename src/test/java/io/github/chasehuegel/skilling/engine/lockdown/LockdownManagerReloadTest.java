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

    @Test
    void reloadThatFailsSkillParseRestoresPreviousResolvers() {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getRegistries()).thenReturn(new Registries(
                new MechanicRegistry(), new TriggerRegistry(), new EvaluatorRegistry()));
        when(plugin.getRequirementEngine()).thenReturn(mock(RequirementEngine.class));
        when(plugin.getSkillEventListener()).thenReturn(mock(io.github.chasehuegel.skilling.engine.listener.SkillEventListener.class));
        when(plugin.getStateFilterRegistry()).thenReturn(
                mock(io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry.class));

        // The previously active resolver set.
        var oldResolver = mock(io.github.chasehuegel.skilling.engine.tag.TagResolver.class);
        var oldEntityResolver = mock(io.github.chasehuegel.skilling.engine.tag.EntityTagResolver.class);
        var oldLoader = mock(io.github.chasehuegel.skilling.engine.tag.CustomTagLoader.class);
        when(plugin.getTagResolver()).thenReturn(oldResolver);
        when(plugin.getEntityTagResolver()).thenReturn(oldEntityResolver);
        when(plugin.getCustomTagLoader()).thenReturn(oldLoader);

        SkillMenuBuilder builder = mock(SkillMenuBuilder.class);
        when(plugin.getSkillMenuBuilder()).thenReturn(builder);

        // loadSkills is the operation that can reject bad YAML after the resolver
        // swap; simulate a malformed skill.
        SkillManager skillManager = mock(SkillManager.class);
        doThrow(new RuntimeException("bad skill")).when(skillManager).loadSkills(any(java.io.File.class));

        AsyncBatchWorker worker = mock(AsyncBatchWorker.class);
        when(worker.flushDirtyProfilesAsync())
                .thenReturn(CompletableFuture.completedFuture(null));

        LockdownManager lockdown = new LockdownManager(plugin,
                mock(ProfileManager.class), worker, skillManager);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getOnlinePlayers()).thenReturn(java.util.List.of());
            assertThrows(RuntimeException.class, lockdown::reload);
        }

        // The surviving (previous) skills must keep running against the previous
        // resolvers, so the swap made during Phase 4 is rolled back.
        verify(plugin).setTagResolver(oldResolver);
        verify(plugin).setEntityTagResolver(oldEntityResolver);
        verify(plugin).setCustomTagLoader(oldLoader);
        verify(skillManager).setTagResolver(oldResolver);
        verify(plugin).setReloading(false);
    }
}
