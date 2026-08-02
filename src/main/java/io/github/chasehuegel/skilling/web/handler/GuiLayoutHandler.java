package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.dto.GuiLayoutDTO;
import io.github.chasehuegel.skilling.web.dto.GuiLayoutSerializer;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * REST handler for the gui.yml configuration file.
 *
 * <p><b>GET /api/gui-layout</b> — Returns the current gui.yml as JSON.
 * Falls back to the default layout if the file doesn't exist.
 *
 * <p><b>PUT /api/gui-layout</b> — Stages a new gui.yml from the JSON request body.
 * The serialized YAML is written to {@code .web_staging/gui.yml} and applied
 * via the standard reload workflow.
 */
public final class GuiLayoutHandler {

    private final StagingManager stagingManager;
    private final File guiFile;

    private static final String GUI_YML = "gui.yml";

    public GuiLayoutHandler(StagingManager stagingManager, File pluginDir) {
        this.stagingManager = stagingManager;
        this.guiFile = new File(pluginDir, GUI_YML);
    }

    /**
     * GET handler: returns the current gui.yml content as a GuiLayoutDTO JSON object.
     * If the file doesn't exist, returns the default layout.
     */
    public void get(Context ctx) {
        try {
            GuiLayoutDTO layout;
            if (guiFile.exists()) {
                String content = Files.readString(guiFile.toPath(), StandardCharsets.UTF_8);
                layout = GuiLayoutSerializer.parse(content);
            } else {
                layout = GuiLayoutDTO.empty();
            }
            ctx.json(layout);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * PUT handler: accepts a GuiLayoutDTO JSON body, validates it, serializes it
     * to YAML, and stages it for the next reload. Returns 400 on invalid input.
     */
    public void update(Context ctx) {
        try {
            GuiLayoutDTO body = ctx.bodyAsClass(GuiLayoutDTO.class);
            String validationError = validate(body);
            if (validationError != null) {
                ctx.status(400).json(Map.of("status", "error", "message", validationError));
                return;
            }
            String yamlContent = GuiLayoutSerializer.serialize(body);
            stagingManager.stageGuiFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private static String validate(GuiLayoutDTO dto) {
        if (dto.rows() < 1 || dto.rows() > 6) {
            return "rows must be between 1 and 6, got " + dto.rows();
        }
        int maxSlot = dto.rows() * 9;
        for (var page : dto.pages()) {
            for (int slot : page.slots().keySet()) {
                if (slot < 0 || slot >= maxSlot) {
                    return "page '" + page.label() + "' has invalid slot " + slot
                            + " (must be 0-" + (maxSlot - 1) + " for " + dto.rows() + " rows)";
                }
            }
        }
        return null;
    }
}
