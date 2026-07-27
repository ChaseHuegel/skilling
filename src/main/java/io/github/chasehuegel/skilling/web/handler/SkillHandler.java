package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO;
import io.github.chasehuegel.skilling.web.dto.SkillSerializer;
import io.github.chasehuegel.skilling.web.dto.SkillSummaryDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SkillHandler {

    private final SkillManager skillManager;
    private final StagingManager stagingManager;
    private final File skillsDir;

    public SkillHandler(SkillManager skillManager, StagingManager stagingManager, File skillsDir) {
        this.skillManager = skillManager;
        this.stagingManager = stagingManager;
        this.skillsDir = skillsDir;
    }

    public void list(Context ctx) {
        List<SkillSummaryDTO> summaries = new ArrayList<>();
        for (var entry : skillManager.getSkills().entrySet()) {
            var def = entry.getValue();
            summaries.add(new SkillSummaryDTO(
                def.id(),
                def.display() != null ? def.display().name() : def.id(),
                def.display() != null ? def.display().icon() : "minecraft:barrier",
                def.display() != null ? def.display().color() : "WHITE",
                def.maxLevel(),
                def.abilities() != null ? def.abilities().size() : 0
            ));
        }
        ctx.json(summaries);
    }

    public void get(Context ctx) {
        String id = ctx.pathParam("id");
        File sourceFile = resolveSkillFile(id);
        if (sourceFile == null) {
            ctx.status(404).json(Map.of("status", "error", "message", "Skill not found: " + id));
            return;
        }

        try {
            SkillDetailDTO dto = SkillSerializer.parseSkillFile(sourceFile);
            ctx.json(dto);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    public void create(Context ctx) {
        try {
            SkillDetailDTO dto = ctx.bodyAsClass(SkillDetailDTO.class);
            validateSkill(dto);
            String yaml = SkillSerializer.toYaml(dto);
            stagingManager.stageSkillFile(dto.id(), yaml);
            ctx.status(201).json(Map.of("status", "ok", "id", dto.id()));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    public void update(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            SkillDetailDTO dto = ctx.bodyAsClass(SkillDetailDTO.class);
            if (!dto.id().equals(id)) {
                throw new IllegalArgumentException("ID in path does not match body");
            }
            validateSkill(dto);
            String yaml = SkillSerializer.toYaml(dto);
            stagingManager.stageSkillFile(id, yaml);
            ctx.json(Map.of("status", "ok", "id", id));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    public void delete(Context ctx) {
        String id = ctx.pathParam("id");
        File liveFile = resolveSkillFile(id);
        File stagedFile = stagingManager.stagedSkillFile(id);

        boolean liveDeleted = liveFile != null && liveFile.delete();
        boolean stagedDeleted = stagedFile.exists() && stagedFile.delete();

        if (liveDeleted || stagedDeleted) {
            ctx.json(Map.of("status", "ok", "id", id));
        } else {
            ctx.status(404).json(Map.of("status", "error", "message", "Skill not found: " + id));
        }
    }

    private File resolveSkillFile(String id) {
        // Fast path: check for file named exactly {id}.yml
        File namedFile = new File(skillsDir, id + ".yml");
        File stagedFile = stagingManager.stagedSkillFile(id);
        if (stagedFile.exists()) return stagedFile;
        if (namedFile.exists()) return namedFile;

        // Fallback: scan all .yml files and match by parsed ID
        // This handles cases where the filename differs from the skill ID
        if (!skillsDir.exists() || !skillsDir.isDirectory()) return null;
        File[] files = skillsDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return null;
        for (File f : files) {
            try {
                SkillDetailDTO dto = SkillSerializer.parseSkillFile(f);
                if (dto.id().equals(id)) return f;
            } catch (Exception ignored) {
                // Skip files that can't be parsed
            }
        }
        return null;
    }

    private static void validateSkill(SkillDetailDTO dto) {
        if (dto.id() == null || dto.id().isBlank()) {
            throw new IllegalArgumentException("Skill id is required");
        }
        if (!dto.id().matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid skill id: must match [a-z_][a-z0-9_]*");
        }
        if (dto.maxLevel() < 1) {
            throw new IllegalArgumentException("maxLevel must be >= 1");
        }
        if (dto.progression() == null) {
            throw new IllegalArgumentException("progression is required");
        }
        if (dto.abilities() != null) {
            var seenIds = new java.util.HashSet<String>();
            for (var a : dto.abilities()) {
                if (a.id() == null || !seenIds.add(a.id())) {
                    throw new IllegalArgumentException("Duplicate or missing ability id: " + a.id());
                }
            }
        }
    }
}
