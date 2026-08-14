package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillHandlerStagingValidationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    private static final String ABILITY_BASE =
            "\"display\":{},\"requirements\":{},\"onFailure\":{},\"feedback\":{},";

    @Test
    void createRejectsUnknownMechanicType() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        SkillHandler handler = new SkillHandler(skillManager, staging, tempDir.resolve("skills").toFile());

        String json = """
            {"id":"test","displayName":"Test","maxLevel":100,"icon":"minecraft:barrier",
             "progression":{"curve":"constant","baseXp":100},
             "xpSources":[],
             "abilities":[{"id":"a","unlockLevel":1,"trigger":"block_break",
                           %s
                           "mechanics":[{"type":"core:nonexistent","parameters":{},"filters":[]}]}]}
            """.formatted(ABILITY_BASE);
        SkillDetailDTO dto = MAPPER.readValue(json, SkillDetailDTO.class);

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(SkillDetailDTO.class)).thenReturn(dto);

        handler.create(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageSkillFile(anyString(), anyString());
    }

    @Test
    void createRejectsUnknownTrigger() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        SkillHandler handler = new SkillHandler(skillManager, staging, tempDir.resolve("skills").toFile());

        String json = """
            {"id":"test","displayName":"Test","maxLevel":100,"icon":"minecraft:barrier",
             "progression":{"curve":"constant","baseXp":100},
             "xpSources":[],
             "abilities":[{"id":"a","unlockLevel":1,"trigger":"not_a_trigger",%s"mechanics":[]}]}
            """.formatted(ABILITY_BASE);
        SkillDetailDTO dto = MAPPER.readValue(json, SkillDetailDTO.class);

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(SkillDetailDTO.class)).thenReturn(dto);

        handler.create(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageSkillFile(anyString(), anyString());
    }

    @Test
    void createRejectsUnknownEvaluatorType() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        SkillHandler handler = new SkillHandler(skillManager, staging, tempDir.resolve("skills").toFile());

        String json = """
            {"id":"test","displayName":"Test","maxLevel":100,"icon":"minecraft:barrier",
             "progression":{"curve":"logistic","baseXp":100},
             "xpSources":[],
             "abilities":[]}
            """;
        SkillDetailDTO dto = MAPPER.readValue(json, SkillDetailDTO.class);

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(SkillDetailDTO.class)).thenReturn(dto);

        handler.create(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageSkillFile(anyString(), anyString());
    }

    @Test
    void renameOntoExistingSkillIdIsRejected() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("farming.yml"), skillYaml("farming"));
        Files.writeString(skillsDir.resolve("mining.yml"), skillYaml("mining"));
        SkillHandler handler = new SkillHandler(skillManager, staging, skillsDir.toFile());

        // Renaming farming -> mining, where mining.yml already lives as a skill.
        SkillDetailDTO dto = MAPPER.readValue(skillJson("mining"), SkillDetailDTO.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.pathParam("id")).thenReturn("farming");
        when(ctx.bodyAsClass(SkillDetailDTO.class)).thenReturn(dto);

        handler.update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageSkillFile(anyString(), anyString());
        verify(staging, never()).stageSkillDeletion(anyString(), anyString());
    }

    @Test
    void renameToAFreeIdStillStages() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("farming.yml"), skillYaml("farming"));
        SkillHandler handler = new SkillHandler(skillManager, staging, skillsDir.toFile());

        // Renaming farming -> mining with no live mining skill must keep working.
        SkillDetailDTO dto = MAPPER.readValue(skillJson("mining"), SkillDetailDTO.class);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.pathParam("id")).thenReturn("farming");
        when(ctx.bodyAsClass(SkillDetailDTO.class)).thenReturn(dto);

        handler.update(ctx);

        verify(staging).stageSkillFile(eq("mining"), anyString());
        verify(staging).stageSkillDeletion(eq("farming"), anyString());
    }

    @Test
    void validationWaitsForReloadRebuild() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = mock(StagingManager.class);
        SkillHandler handler = new SkillHandler(skillManager, staging, tempDir.resolve("skills").toFile());

        SkillDetailDTO dto = MAPPER.readValue(skillJson("test"), SkillDetailDTO.class);

        // Simulate the reload rebuild holding the registry write lock while it
        // clears/re-populates the shared registries.
        var writeLock = skillManager.registryLock().writeLock();
        writeLock.lock();

        var saveDone = new java.util.concurrent.CompletableFuture<Void>();
        Thread saver = new Thread(() -> {
            Context ctx = mock(Context.class, RETURNS_SELF);
            when(ctx.bodyAsClass(SkillDetailDTO.class)).thenReturn(dto);
            handler.create(ctx);
            saveDone.complete(null);
        });
        saver.start();

        // The validation must block (read lock unavailable) while the rebuild
        // holds the write lock, so it can never observe emptied registries.
        boolean blocked = !saveDone.isDone();
        assertTrue(blocked, "validation must serialize against the reload rebuild");
        Thread.sleep(200);
        assertFalse(saveDone.isDone(), "validation must stay blocked while the rebuild runs");

        // The rebuild finishes; validation proceeds and succeeds for valid content.
        writeLock.unlock();
        saver.join(5000);
        assertTrue(saveDone.isDone(), "validation must complete once the rebuild releases the lock");
        verify(staging).stageSkillFile(eq("test"), anyString());
    }

    @Test
    void getPrefersStagedFileOverLive() throws Exception {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        StagingManager staging = new StagingManager(tempDir.toFile());
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("mining.yml"), skillYaml("mining"));
        SkillHandler handler = new SkillHandler(skillManager, staging, skillsDir.toFile());

        // A pending edit changes the max level; the editor must show the staged
        // content, not the stale live file.
        staging.stageSkillFile("mining", skillYaml("mining").replace("max_level: 100", "max_level: 50"));

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.pathParam("id")).thenReturn("mining");
        handler.get(ctx);

        SkillDetailDTO dto = (SkillDetailDTO) jsonArg(ctx);
        assertEquals(50, dto.maxLevel(), "get must prefer the staged skill over the live file");
    }

    private Object jsonArg(Context ctx) {
        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        return captor.getValue();
    }

    private static String skillYaml(String id) {
        return """
                id: "%s"
                max_level: 100
                display:
                  name: "%s"
                  icon: "minecraft:barrier"
                  color: "GREEN"
                  style: "SOLID"
                progression:
                  curve: "constant"
                  base_xp: 100
                xp_sources: []
                abilities: []
                """.formatted(id, id);
    }

    private static String skillJson(String id) {
        return """
            {"id":"%s","displayName":"%s","maxLevel":100,"icon":"minecraft:barrier",
             "progression":{"curve":"constant","baseXp":100},
             "xpSources":[],
             "abilities":[]}
            """.formatted(id, id);
    }
}
