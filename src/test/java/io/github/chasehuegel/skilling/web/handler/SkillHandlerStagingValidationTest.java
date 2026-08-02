package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
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
}
