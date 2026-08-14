package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillHandlerSecurityTest {

    @TempDir
    Path tempDir;

    private File skillsDir() {
        File dir = new File(tempDir.toFile(), "skills");
        dir.mkdirs();
        return dir;
    }

    private static Context mockContext(String id) {
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.pathParam("id")).thenReturn(id);
        return ctx;
    }

    private static String invalidIdMessage() {
        return "Invalid skill id: must match [a-z_][a-z0-9_]*";
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../config",
        "../evil",
        "..%2F..%2Fconfig",
        "config/../../etc/passwd",
        "a/../b",
        "mining/..",
        ".hidden",
        "UPPER",
        "has space",
        "with-dash"
    })
    void getRejectsTraversalOrInvalidIds(String decodedId) {
        // A real escaped target that would throw if parsed proves the file is never read.
        File escaped = new File(tempDir.toFile(), "evil.yml");
        write(escaped, "id: [broken yaml");

        Context ctx = mockContext(decodedId);

        new SkillHandler(null, new StagingManager(tempDir.toFile()), skillsDir()).get(ctx);

        verify(ctx).status(400);
        verify(ctx, never()).json(any(SkillDetailDTO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../config", "../evil", "..%2F..%2Fconfig"})
    void updateRejectsTraversalIdBeforeParsingBody(String decodedId) {
        Context ctx = mockContext(decodedId);

        new SkillHandler(null, new StagingManager(tempDir.toFile()), skillsDir()).update(ctx);

        verify(ctx).status(400);
        verify(ctx, never()).bodyAsClass(any(Class.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../config", "../evil", "..%2F..%2Fconfig"})
    void deleteRejectsTraversalIdWithoutStagingDeletion(String decodedId) {
        Context ctx = mockContext(decodedId);
        StagingManager staging = mock(StagingManager.class);

        new SkillHandler(null, staging, skillsDir()).delete(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageSkillDeletion(anyString(), anyString());
    }

    @Test
    void getReturnsLegitSkill() throws IOException {
        writeSkill("mining.yml");

        Context ctx = mockContext("mining");

        new SkillHandler(null, new StagingManager(tempDir.toFile()), skillsDir()).get(ctx);

        verify(ctx, never()).status(400);
        verify(ctx, never()).status(404);
        var captor = org.mockito.ArgumentCaptor.forClass(SkillDetailDTO.class);
        verify(ctx).json(captor.capture());
        assertNotNull(captor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals("mining", captor.getValue().id());
    }

    @Test
    void getReturnsSkillNestedInSubfolder() throws IOException {
        writeSkillWithLevel("special/blasting.yml", "blasting", 100);

        Context ctx = mockContext("blasting");

        new SkillHandler(null, new StagingManager(tempDir.toFile()), skillsDir()).get(ctx);

        verify(ctx, never()).status(400);
        verify(ctx, never()).status(404);
        var captor = org.mockito.ArgumentCaptor.forClass(SkillDetailDTO.class);
        verify(ctx).json(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("blasting", captor.getValue().id());
    }

    @Test
    void getNestedSkillReturns404WhenIdDoesNotExist() throws IOException {
        writeSkillWithLevel("special/blasting.yml", "blasting", 100);

        Context ctx = mockContext("missing");

        new SkillHandler(null, new StagingManager(tempDir.toFile()), skillsDir()).get(ctx);

        verify(ctx).status(404);
        verify(ctx, never()).json(any(SkillDetailDTO.class));
    }

    @Test
    void getDuplicateIdResolvesToTheFileTheEngineLoadedFirst() throws IOException {
        // Sorted relative-path order: a.yml before special/blasting.yml, so the
        // engine keeps a.yml's definition for id "blasting"; the lookup must
        // return the same file (max_level 100), not the later duplicate.
        writeSkillWithLevel("a.yml", "blasting", 100);
        writeSkillWithLevel("special/blasting.yml", "blasting", 200);

        Context ctx = mockContext("blasting");

        new SkillHandler(null, new StagingManager(tempDir.toFile()), skillsDir()).get(ctx);

        verify(ctx, never()).status(404);
        var captor = org.mockito.ArgumentCaptor.forClass(SkillDetailDTO.class);
        verify(ctx).json(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(100, captor.getValue().maxLevel());
    }

    @Test
    void deleteResolvesNestedSkill() {
        writeSkillWithLevel("special/blasting.yml", "blasting", 100);
        StagingManager staging = new StagingManager(tempDir.toFile());

        Context ctx = mockContext("blasting");

        new SkillHandler(null, staging, skillsDir()).delete(ctx);

        verify(ctx).json(Map.of("status", "ok", "id", "blasting"));
        org.junit.jupiter.api.Assertions.assertTrue(
            new File(staging.getStagingDir(), "deleted_skills/blasting.yml.deleted").exists());
    }

    @Test
    void deleteNestedSkillRemovesTheLiveFileAfterApply() {
        writeSkillWithLevel("special/blasting.yml", "blasting", 100);
        StagingManager staging = new StagingManager(tempDir.toFile());

        new SkillHandler(null, staging, skillsDir()).delete(mockContext("blasting"));
        staging.applyAndBackup();

        assertFalse(new File(skillsDir(), "special/blasting.yml").exists(),
                "applying a nested-skill deletion must remove the real nested file");
    }

    @Test
    void deleteFlatSkillStillRemovesTheFlatFileAfterApply() {
        writeSkill("mining.yml");
        StagingManager staging = new StagingManager(tempDir.toFile());

        new SkillHandler(null, staging, skillsDir()).delete(mockContext("mining"));
        staging.applyAndBackup();

        assertFalse(new File(skillsDir(), "mining.yml").exists(),
                "a flat-path deletion must behave exactly as before");
    }

    @Test
    void renameOutOfSubfolderRemovesTheOriginalNestedFileAfterApply() throws Exception {
        writeSkillWithLevel("special/blasting.yml", "blasting", 100);
        StagingManager staging = new StagingManager(tempDir.toFile());
        io.github.chasehuegel.skilling.engine.SkillManager skillManager =
                io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
        SkillHandler handler = new SkillHandler(skillManager, staging, skillsDir());

        String json = """
            {"id":"mining","displayName":"Mining","maxLevel":100,"icon":"minecraft:barrier",
             "progression":{"curve":"constant","baseXp":100},"xpSources":[],"abilities":[]}
            """;
        io.github.chasehuegel.skilling.web.dto.SkillDetailDTO dto =
                new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.pathParam("id")).thenReturn("blasting");
        when(ctx.bodyAsClass(io.github.chasehuegel.skilling.web.dto.SkillDetailDTO.class)).thenReturn(dto);

        handler.update(ctx);
        staging.applyAndBackup();

        assertFalse(new File(skillsDir(), "special/blasting.yml").exists(),
                "renaming out of a subfolder must remove the original nested file");
        assertTrue(new File(skillsDir(), "mining.yml").exists(),
                "the new flat file must exist after apply");
    }

    @Test
    void deleteLegitIdStagesDeletion() {
        writeSkill("mining.yml");
        StagingManager staging = new StagingManager(tempDir.toFile());

        Context ctx = mockContext("mining");

        new SkillHandler(null, staging, skillsDir()).delete(ctx);

        verify(ctx).json(Map.of("status", "ok", "id", "mining"));
        org.junit.jupiter.api.Assertions.assertTrue(
            new File(staging.getStagingDir(), "deleted_skills/mining.yml.deleted").exists());
    }

    @Test
    void confineToRejectsPathsOutsideBase() {
        File base = tempDir.toFile();
        File inside = new File(base, "skills/../skills/mining.yml");
        File escaped = new File(base, "../secret.yml");

        assertNotNull(SkillHandler.confineTo(base.toPath(), inside));
        assertNull(SkillHandler.confineTo(base.toPath(), escaped));
    }

    private void writeSkill(String name) {
        writeSkillWithLevel(name, "mining", 100);
    }

    private void writeSkillWithLevel(String relative, String id, int maxLevel) {
        File file = new File(skillsDir(), relative);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        write(file, """
            id: %s
            max_level: %d
            display_name: %s
            progression:
              curve: polynomial
              base_xp: 50
              exponent: 2.5
            """.formatted(id, maxLevel, id));
    }

    private void write(File file, String content) {
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
