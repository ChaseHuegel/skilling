package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.web.dto.SkillDetailDTO;
import io.github.chasehuegel.skilling.web.dto.SkillSerializer;
import io.github.chasehuegel.skilling.web.dto.SkillSummaryDTO;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class SkillHandler {

    private static final Logger LOGGER = Logger.getLogger(SkillHandler.class.getName());

    private static final Pattern VALID_SKILL_ID = Pattern.compile("[a-z_][a-z0-9_]*");
    private static final String INVALID_ID_MESSAGE = "Invalid skill id: must match [a-z_][a-z0-9_]*";

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
            List<String> xpSourceTriggers = def.xpSources() != null
                ? def.xpSources().stream().map(SkillDefinition.XpSource::trigger).toList()
                : Collections.emptyList();
            List<String> abilityIds = def.abilities() != null
                ? def.abilities().stream().map(SkillDefinition.Ability::id).toList()
                : Collections.emptyList();
            List<String> abilityNames = def.abilities() != null
                ? def.abilities().stream().map(a -> a.displayName() != null ? a.displayName() : a.id()).toList()
                : Collections.emptyList();
            summaries.add(new SkillSummaryDTO(
                def.id(),
                def.display() != null ? def.display().name() : def.id(),
                def.display() != null ? def.display().icon() : "minecraft:barrier",
                def.display() != null ? def.display().color() : "WHITE",
                def.maxLevel(),
                def.abilities() != null ? def.abilities().size() : 0,
                def.xpSources() != null ? def.xpSources().size() : 0,
                xpSourceTriggers,
                abilityIds,
                abilityNames
            ));
        }
        ctx.json(summaries);
    }

    public void get(Context ctx) {
        String id = ctx.pathParam("id");
        if (!isValidIdParam(ctx, id)) return;
        File sourceFile = resolveSkillFile(id);
        if (sourceFile == null) {
            ctx.status(404).json(Map.of("status", "error", "message", "Skill not found: " + id));
            return;
        }

        try {
            SkillDetailDTO dto = SkillSerializer.parseSkillFile(sourceFile);
            ctx.json(dto);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to parse skill file: " + id, e);
        }
    }

    public void create(Context ctx) {
        try {
            SkillDetailDTO dto = WebError.parseBody(ctx, SkillDetailDTO.class);
            validateSkill(dto);
            String yaml = SkillSerializer.toYaml(dto);
            validateStagedSkill(yaml);
            stagingManager.stageSkillFile(dto.id(), yaml);
            ctx.status(201).json(Map.of("status", "ok", "id", dto.id()));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        } catch (JsonProcessingException e) {
            WebError.malformedJson(ctx);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to create skill", e);
        }
    }

    public void update(Context ctx) {
        String oldId = ctx.pathParam("id");
        if (!isValidIdParam(ctx, oldId)) return;
        try {
            SkillDetailDTO dto = WebError.parseBody(ctx, SkillDetailDTO.class);
            validateSkill(dto);
            String newId = dto.id();
            String yaml = SkillSerializer.toYaml(dto);
            validateStagedSkill(yaml);
            if (!newId.equals(oldId)) {
                // Renaming onto an ID that a different live skill already claims
                // would silently overwrite its file on Apply. Reject up front
                // rather than letting the reload destroy that skill. Renaming to
                // a deleted or never-existing ID (no live file) keeps working.
                File oldFile = resolveSkillFile(oldId);
                File colliding = liveSkillFile(newId);
                if (colliding != null && !colliding.equals(oldFile)) {
                    throw new IllegalArgumentException("Skill id already exists: " + newId);
                }
                stagingManager.stageSkillFile(newId, yaml);
                stagingManager.stageSkillDeletion(oldId);
            } else {
                stagingManager.stageSkillFile(oldId, yaml);
            }
            ctx.json(Map.of("status", "ok", "id", newId));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        } catch (JsonProcessingException e) {
            WebError.malformedJson(ctx);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to update skill", e);
        }
    }

    public void delete(Context ctx) {
        String id = ctx.pathParam("id");
        if (!isValidIdParam(ctx, id)) return;
        File liveFile = resolveSkillFile(id);
        File stagedFile = stagingManager.stagedSkillFile(id);

        if (liveFile == null && !stagedFile.exists()) {
            ctx.status(404).json(Map.of("status", "error", "message", "Skill not found: " + id));
            return;
        }

        stagingManager.stageSkillDeletion(id);
        ctx.json(Map.of("status", "ok", "id", id));
    }

    private File resolveSkillFile(String id) {
        // Prefer the staged file so the editor shows pending edits after a save
        // (consistent with the create path); fall back to the live file when
        // nothing is staged.
        File stagedFile = stagingManager.stagedSkillFile(id);
        if (stagedFile != null && stagedFile.exists()) return stagedFile;
        return liveSkillFile(id);
    }

    /**
     * Finds the live skill file for an id: a file named exactly {@code {id}.yml},
     * or a live file whose parsed skill id matches {@code id} (handles files whose
     * name differs from the skill id). Staged files are ignored, so this answers
     * "does a live skill with this id already exist?" for collision checks.
     *
     * @param id the skill id
     * @return the matching live file, or null
     */
    private File liveSkillFile(String id) {
        File namedFile = confinedLiveFile(id);
        if (namedFile != null && namedFile.exists()) return namedFile;
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

    /**
     * Builds the live skill file for an id and verifies the resolved path stays
     * within {@code skillsDir}. Defense-in-depth against any future param source
     * that bypasses {@link #isValidIdParam}: an out-of-directory result is never
     * handed to callers.
     */
    private File confinedLiveFile(String id) {
        return confineTo(skillsDir.toPath(), new File(skillsDir, id + ".yml"));
    }

    static File confineTo(Path base, File file) {
        try {
            Path resolvedBase = base.toRealPath();
            Path candidate = file.toPath().toAbsolutePath().normalize();
            if (candidate.startsWith(resolvedBase)) {
                return candidate.toFile();
            }
            LOGGER.warning("Rejected path outside base directory: " + candidate);
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isValidIdParam(Context ctx, String id) {
        if (id == null || !VALID_SKILL_ID.matcher(id).matches()) {
            ctx.status(400).json(Map.of("status", "error", "message", INVALID_ID_MESSAGE));
            return false;
        }
        return true;
    }

    /**
     * Rejects staged skill YAML that references unknown triggers, mechanics,
     * evaluators, or tags by parsing it through the live {@link SkillManager},
     * so malformed content is caught before it can be applied by a reload.
     */
    private void validateStagedSkill(String yaml) {
        if (skillManager == null) return;
        // Take the registry read lock so a concurrent reload rebuild (which holds
        // the write lock while it clears/re-populates the shared registries) never
        // empties them mid-parse, which would spuriously reject valid content.
        java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock readLock = skillManager.registryLock().readLock();
        readLock.lock();
        try {
            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.StringReader(yaml));
            skillManager.parseSkill(config);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid skill YAML: " + e.getMessage(), e);
        } finally {
            readLock.unlock();
        }
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
