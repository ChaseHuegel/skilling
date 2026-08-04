package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.dto.GuiLayoutDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void updateRejectsReservedNavigationSlot() {
        StagingManager staging = mock(StagingManager.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        // The last row (slots 27/31/35 in a 4-row layout) is reserved for the
        // page arrows and indicator; the engine silently drops skills placed there.
        GuiLayoutDTO dto = new GuiLayoutDTO("Test", 4, List.of(
            new GuiLayoutDTO.GuiPageDTO("Combat", Map.of(31, "swords"), "minecraft:book", 0)
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
            new GuiLayoutDTO.GuiPageDTO("Combat", Map.of(0, "swords", 34, "archery"), "minecraft:book", 0)
        ), 2);
        when(ctx.bodyAsClass(GuiLayoutDTO.class)).thenReturn(dto);

        handlerWith(staging).update(ctx);

        verify(ctx, never()).status(400);
        verify(staging).stageGuiFile(anyString());
        verify(ctx).json(Map.of("status", "ok"));
    }

    @Test
    void getPrefersStagedLayoutOverLive() throws Exception {
        StagingManager staging = new StagingManager(tempDir.toFile());
        java.nio.file.Files.writeString(tempDir.resolve("gui.yml"),
                "rows: 3\npages: []\n");
        // A pending edit changes the row count; the editor must show the staged layout.
        staging.stageGuiFile("rows: 6\npages: []\n");

        Context ctx = mock(Context.class, RETURNS_SELF);
        handlerWith(staging).get(ctx);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        GuiLayoutDTO dto = (GuiLayoutDTO) captor.getValue();
        assertEquals(6, dto.rows(), "get must prefer the staged layout over the live file");
    }

    @Test
    void getReturnsLiveLayoutWhenNothingStaged() throws Exception {
        StagingManager staging = new StagingManager(tempDir.toFile());
        java.nio.file.Files.writeString(tempDir.resolve("gui.yml"),
                "rows: 3\npages: []\n");

        Context ctx = mock(Context.class, RETURNS_SELF);
        handlerWith(staging).get(ctx);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        GuiLayoutDTO dto = (GuiLayoutDTO) captor.getValue();
        assertEquals(3, dto.rows(), "without a staged edit the live layout must be served");
    }
}
