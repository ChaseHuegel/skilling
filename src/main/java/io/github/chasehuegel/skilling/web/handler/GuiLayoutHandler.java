package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.chasehuegel.skilling.web.dto.GuiLayoutDTO;
import io.github.chasehuegel.skilling.web.dto.GuiLayoutSerializer;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(GuiLayoutHandler.class.getName());

    private final StagingManager stagingManager;
    private final File guiFile;

    private static final String GUI_YML = "gui.yml";

    public GuiLayoutHandler(StagingManager stagingManager, File pluginDir) {
        this.stagingManager = stagingManager;
        this.guiFile = new File(pluginDir, GUI_YML);
    }

    /**
     * GET handler: returns the current gui.yml content as a GuiLayoutDTO JSON object.
     * A staged edit (pending reload) is shown in preference to the live file so the
     * editor never re-saves against stale content. If no file exists, returns the
     * default layout.
     */
    public void get(Context ctx) {
        try {
            // Prefer the staged layout so pending edits stay visible after a save;
            // fall back to the live file when nothing is staged.
            File staged = stagingManager.stagedGuiFile();
            File source = staged.exists() ? staged : guiFile;
            GuiLayoutDTO layout;
            if (source.exists()) {
                String content = Files.readString(source.toPath(), StandardCharsets.UTF_8);
                layout = GuiLayoutSerializer.parse(content);
            } else {
                layout = GuiLayoutDTO.empty();
            }
            ctx.json(layout);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to read " + GUI_YML, e);
        }
    }

    /**
     * PUT handler: accepts a GuiLayoutDTO JSON body, validates it, serializes it
     * to YAML, and stages it for the next reload. Returns 400 on invalid input.
     */
    public void update(Context ctx) {
        try {
            GuiLayoutDTO body = WebError.parseBody(ctx, GuiLayoutDTO.class);
            String validationError = validate(body);
            if (validationError != null) {
                WebError.badRequest(ctx, validationError);
                return;
            }
            String yamlContent = GuiLayoutSerializer.serialize(body);
            stagingManager.stageGuiFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (JsonProcessingException e) {
            WebError.malformedJson(ctx);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to stage " + GUI_YML, e);
        }
    }

    private static String validate(GuiLayoutDTO dto) {
        if (dto.rows() < 1 || dto.rows() > 6) {
            return "rows must be between 1 and 6, got " + dto.rows();
        }
        int maxSlot = dto.rows() * 9;
        // The navigation row (last row) is auto-reserved for the arrows and page
        // indicator; the engine drops any skill assigned there, so reject it here.
        int lastRowStart = (dto.rows() - 1) * 9;
        var reserved = java.util.Set.of(lastRowStart, lastRowStart + 4, lastRowStart + 8);
        for (var page : dto.pages()) {
            Set<Integer> used = new java.util.HashSet<>();
            for (int slot : page.slots().keySet()) {
                if (slot < 0 || slot >= maxSlot) {
                    return "page '" + page.label() + "' has invalid slot " + slot
                            + " (must be 0-" + (maxSlot - 1) + " for " + dto.rows() + " rows)";
                }
                if (reserved.contains(slot)) {
                    return "page '" + page.label() + "' assigns a skill to reserved navigation slot "
                            + slot + " (the last row holds the page controls)";
                }
                if (!used.add(slot)) {
                    return "page '" + page.label() + "' has duplicate slot " + slot;
                }
            }
        }
        return null;
    }
}
