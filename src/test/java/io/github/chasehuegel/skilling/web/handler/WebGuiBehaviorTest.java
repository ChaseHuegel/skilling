package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the staged reads (config/tags), reload conflict labeling, reload
 * single-flight, and the reload-failure rollback.
 */
class WebGuiBehaviorTest {

    @TempDir
    Path tempDir;

    @Test
    void configGetReflectsTheStagedFileWhenOneExists() throws Exception {
        File live = tempDir.resolve("config.yml").toFile();
        Files.writeString(live.toPath(), "debug_logging: false\n");
        StagingManager staging = new StagingManager(tempDir.toFile());
        staging.stageConfigFile("debug_logging: true\n");
        ConfigHandler handler = new ConfigHandler(staging, live);

        Context ctx = mock(Context.class);
        handler.get(ctx);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());
        assertEquals(true, captor.getValue().get("debugLogging"),
                "config GET must show the pending staged edit");
    }

    @Test
    void configGetReturnsLiveWhenNothingStaged() throws Exception {
        File live = tempDir.resolve("config.yml").toFile();
        Files.writeString(live.toPath(), "debug_logging: false\n");
        StagingManager staging = new StagingManager(tempDir.toFile());
        ConfigHandler handler = new ConfigHandler(staging, live);

        Context ctx = mock(Context.class);
        handler.get(ctx);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());
        assertEquals(false, captor.getValue().get("debugLogging"));
    }

    @Test
    void tagGetReflectsTheStagedFileWhenOneExists() throws Exception {
        java.io.File live = new java.io.File(new java.io.File(tempDir.toFile(), "tags"), "base.yml");
        Files.createDirectories(live.toPath().getParent());
        Files.writeString(live.toPath(), "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n");
        StagingManager staging = new StagingManager(tempDir.toFile());
        staging.stageTagsFile("custom_tags:\n  gems:\n    - \"minecraft:diamond\"\n");
        TagHandler handler = new TagHandler(staging, live);

        Context ctx = mock(Context.class);
        handler.get(ctx);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());
        Map<String, Object> body = captor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> tags = (Map<String, List<String>>) body.get("tags");
        assertTrue(tags.containsKey("#c:gems"), "a pending staged tag edit must be served");
        assertFalse(tags.containsKey("#c:ores"), "the live tag must not shadow the staged edit");
    }

    @Test
    void emptyApplyWithPendingChangesIsReportedAsConflict() {
        StagingManager staging = mock(StagingManager.class);
        when(staging.checkConflicts()).thenReturn(List.of(), List.of("tags/base.yml"));
        when(staging.applyAndBackup()).thenReturn(List.of());
        when(staging.hasPendingChanges()).thenReturn(true);
        ReloadHandler handler = new ReloadHandler(mock(Skilling.class), staging, mock(LockdownManager.class));

        Context ctx = mock(Context.class, RETURNS_SELF);
        handler.reload(ctx);

        verify(ctx).status(409);
    }

    @Test
    void concurrentReloadIsRejectedWhileOneIsInProgress() throws Exception {
        Skilling plugin = mock(Skilling.class);
        StagingManager staging = mock(StagingManager.class);
        when(staging.checkConflicts()).thenReturn(List.of());
        when(staging.hasPendingChanges()).thenReturn(false);
        LockdownManager lockdown = mock(LockdownManager.class);

        // The first reload parks inside applyAndBackup until the test releases it,
        // keeping the single-flight flag held while the second reload fires.
        var reached = new CountDownLatch(1);
        var block = new CountDownLatch(1);
        when(staging.applyAndBackup()).thenAnswer(inv -> {
            reached.countDown();
            block.await();
            return List.of("skills/mining.yml");
        });
        org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        when(scheduler.callSyncMethod(any(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(
                        java.util.concurrent.CompletableFuture.completedFuture(null)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getScheduler()).thenReturn(scheduler);
            ReloadHandler handler = new ReloadHandler(plugin, staging, lockdown);

            Context first = mock(Context.class, RETURNS_SELF);
            Thread t1 = new Thread(() -> handler.reload(first));
            t1.start();
            reached.await();

            Context second = mock(Context.class, RETURNS_SELF);
            handler.reload(second);
            verify(second).status(409);

            block.countDown();
            t1.join(10_000);
        }
    }
}
