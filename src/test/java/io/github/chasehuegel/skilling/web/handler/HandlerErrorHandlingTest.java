package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonParseException;
import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.web.dto.GuiLayoutDTO;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandlerErrorHandlingTest {

    @TempDir
    Path tempDir;

    private static JsonParseException malformedJson() {
        return new JsonParseException(null, "malformed json");
    }

    @Test
    void skillCreateMalformedJsonReturns400() {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(SkillDetailDTO.class)).thenAnswer(inv -> { throw malformedJson(); });

        new SkillHandler(skillManager, staging, tempDir.resolve("skills").toFile()).create(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageSkillFile(anyString(), anyString());
    }

    @Test
    void guiLayoutUpdateMalformedJsonReturns400() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(GuiLayoutDTO.class)).thenAnswer(inv -> { throw malformedJson(); });

        new GuiLayoutHandler(staging, tempDir.toFile()).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageGuiFile(anyString());
    }

    @Test
    void tagUpdateMalformedJsonReturns400() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenAnswer(inv -> { throw malformedJson(); });

        new TagHandler(staging, tempDir.resolve("tags").resolve("base.yml").toFile()).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageTagsFile(anyString());
    }

    @Test
    void tagUpdateWrongShapeReturns400() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of("tags", "not-a-map"));

        new TagHandler(staging, tempDir.resolve("tags").resolve("base.yml").toFile()).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageTagsFile(anyString());
    }

    @Test
    void tagUpdateTagValueNotAListReturns400() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of("tags", Map.of("#c:ores", "not-a-list")));

        new TagHandler(staging, tempDir.resolve("tags").resolve("base.yml").toFile()).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageTagsFile(anyString());
    }

    @Test
    void configUpdateWrongShapeReturns400() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of("database", "not-a-map"));

        new ConfigHandler(staging, tempDir.resolve("config.yml").toFile()).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void internalErrorNeverLeaksExceptionMessage() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of("tags", Map.of("#c:ores", List.of("minecraft:iron_ore"))));
        doThrow(new RuntimeException("leaky /tmp/plugins/Skilling/tags.yml detail"))
            .when(staging).stageTagsFile(anyString());

        new TagHandler(staging, tempDir.resolve("tags").resolve("base.yml").toFile()).update(ctx);

        verify(ctx).status(500);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());
        Map<String, Object> body = captor.getValue();
        assertEquals("Internal server error", body.get("message"));
        assertFalse(String.valueOf(body).contains("leaky"), "response must not leak exception detail");
    }

    @Test
    @SuppressWarnings("unchecked")
    void reloadNeverEmitsNullOrConfidentialMessage() throws Exception {
        StagingManager staging = mock(StagingManager.class);
        when(staging.checkConflicts()).thenReturn(List.of());
        when(staging.applyAndBackup()).thenReturn(List.of("gui.yml"));
        when(staging.hasPendingChanges()).thenReturn(false);

        LockdownManager lockdown = mock(LockdownManager.class);
        Skilling plugin = mock(Skilling.class);
        Context ctx = mock(Context.class, RETURNS_SELF);

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.callSyncMethod(any(Skilling.class), any(Callable.class)))
            .thenAnswer(inv -> { throw new ExecutionException(new RuntimeException("confidential lockdown detail")); });

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            new ReloadHandler(plugin, staging, lockdown).reload(ctx);
        }

        verify(ctx).status(500);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());
        Map<String, Object> body = captor.getValue();
        assertEquals(List.of("Reload lockdown failed"), body.get("errors"));
        assertFalse(String.valueOf(body).contains("confidential"), "response must not leak reload exception");
        assertFalse(String.valueOf(body).contains("null"), "response must never emit a null message");
    }
}
