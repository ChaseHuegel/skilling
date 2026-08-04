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
        verify(staging, never()).stageSkillDeletion(anyString());
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
        verify(staging).stageSkillDeletion(eq("farming"));
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
