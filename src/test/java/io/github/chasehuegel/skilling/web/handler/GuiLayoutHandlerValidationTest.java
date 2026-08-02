package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.dto.GuiLayoutDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuiLayoutHandlerValidationTest {

    @TempDir
    Path tempDir;

    private GuiLayoutHandler handlerWith(StagingManager staging) {
        return new GuiLayoutHandler(staging, tempDir.toFile());
    }

    @Test
    void updateRejectsRowsBelowRange() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(GuiLayoutDTO.class)).thenReturn(new GuiLayoutDTO("Test", 0, List.of(), 1));

        handlerWith(staging).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageGuiFile(anyString());
    }

    @Test
    void updateRejectsRowsAboveRange() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(GuiLayoutDTO.class)).thenReturn(new GuiLayoutDTO("Test", 9, List.of(), 1));

        handlerWith(staging).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageGuiFile(anyString());
    }

    @Test
    void updateRejectsOutOfRangeSlot() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 4, List.of(
            new GuiLayoutDTO.GuiPageDTO("Combat", Map.of(40, "swords"), "minecraft:book", 0)
        ), 1);
        when(ctx.bodyAsClass(GuiLayoutDTO.class)).thenReturn(dto);

        handlerWith(staging).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageGuiFile(anyString());
    }

    @Test
    void updateStagesValidLayout() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 4, List.of(
            new GuiLayoutDTO.GuiPageDTO("Combat", Map.of(0, "swords", 35, "archery"), "minecraft:book", 0)
        ), 2);
        when(ctx.bodyAsClass(GuiLayoutDTO.class)).thenReturn(dto);

        handlerWith(staging).update(ctx);

        verify(ctx, never()).status(400);
        verify(staging).stageGuiFile(anyString());
        verify(ctx).json(Map.of("status", "ok"));
    }
}
