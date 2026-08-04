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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LockdownManagerReloadTest {

    @TempDir
    Path tempDir;

    /** A scheduler that runs main-thread tasks synchronously so the async reload is deterministic. */
    private static BukkitScheduler syncScheduler() {
        var scheduler = mock(BukkitScheduler.class);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mock(BukkitTask.class);
        }).when(scheduler).runTask(any(Plugin.class), any(Runnable.class));
        return scheduler;
    }

    private static MockedStatic<Bukkit> mockBukkit() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        when(Bukkit.getOnlinePlayers()).thenReturn(java.util.List.of());
        var scheduler = syncScheduler();
        when(Bukkit.getScheduler()).thenReturn(scheduler);
        return bukkit;
    }

    /** A SkillManager mock whose registry lock is functional (rebuild takes the write lock). */
    private static SkillManager skillManagerMock() {
        SkillManager skillManager = mock(SkillManager.class);
        when(skillManager.registryLock())
                .thenReturn(new java.util.concurrent.locks.ReentrantReadWriteLock());
        return skillManager;
    }

    @Test
    void reloadThatThrowsNeverLeavesPluginReloading() throws Exception {
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
                mock(ProfileManager.class), worker, mock(SkillManager.class), Runnable::run);

        try (MockedStatic<Bukkit> bukkit = mockBukkit()) {
            // Phase 5 throws; the future fails but the finally must still unlock.
            CompletableFuture<Void> reload = lockdown.reloadAsync();
            assertThrows(ExecutionException.class,
                    () -> reload.get(5, TimeUnit.SECONDS));
        }

        verify(plugin).setReloading(false);
    }

    @Test
    void reloadThatFailsSkillParseRestoresPreviousResolvers() throws Exception {
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
        SkillManager skillManager = skillManagerMock();
        doThrow(new RuntimeException("bad skill")).when(skillManager).loadSkills(any(java.io.File.class));

        AsyncBatchWorker worker = mock(AsyncBatchWorker.class);
        when(worker.flushDirtyProfilesAsync())
                .thenReturn(CompletableFuture.completedFuture(null));

        LockdownManager lockdown = new LockdownManager(plugin,
                mock(ProfileManager.class), worker, skillManager, Runnable::run);

        try (MockedStatic<Bukkit> bukkit = mockBukkit()) {
            CompletableFuture<Void> reload = lockdown.reloadAsync();
            assertThrows(ExecutionException.class,
                    () -> reload.get(5, TimeUnit.SECONDS));
        }

        // The surviving (previous) skills must keep running against the previous
        // resolvers, so the swap made during the rebuild is rolled back.
        verify(plugin).setTagResolver(oldResolver);
        verify(plugin).setEntityTagResolver(oldEntityResolver);
        verify(plugin).setCustomTagLoader(oldLoader);
        verify(skillManager).setTagResolver(oldResolver);
        verify(plugin).setReloading(false);
    }

    @Test
    void reloadClearsTransientAttributeModifiers() throws Exception {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getRegistries()).thenReturn(new Registries(
                new MechanicRegistry(), new TriggerRegistry(), new EvaluatorRegistry()));
        when(plugin.getRequirementEngine()).thenReturn(mock(RequirementEngine.class));
        when(plugin.getSkillEventListener()).thenReturn(mock(io.github.chasehuegel.skilling.engine.listener.SkillEventListener.class));

        SkillMenuBuilder builder = mock(SkillMenuBuilder.class);
        when(plugin.getSkillMenuBuilder()).thenReturn(builder);

        AsyncBatchWorker worker = mock(AsyncBatchWorker.class);
        when(worker.flushDirtyProfilesAsync())
                .thenReturn(CompletableFuture.completedFuture(null));

        LockdownManager lockdown = new LockdownManager(plugin,
                mock(ProfileManager.class), worker, skillManagerMock(), Runnable::run);

        try (MockedStatic<Bukkit> bukkit = mockBukkit();
             MockedStatic<io.github.chasehuegel.skilling.engine.mechanic.impl.AttributeModifierHelper> helper =
                     mockStatic(io.github.chasehuegel.skilling.engine.mechanic.impl.AttributeModifierHelper.class)) {
            lockdown.reloadAsync().get(5, TimeUnit.SECONDS);
            // Phase 5 invalidation must strip transient attribute modifiers on reload.
            helper.verify(() -> io.github.chasehuegel.skilling.engine.mechanic.impl.AttributeModifierHelper.clearAll());
        }
    }
}
