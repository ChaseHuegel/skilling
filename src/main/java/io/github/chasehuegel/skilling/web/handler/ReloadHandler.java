package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReloadHandler {

    private final StagingManager stagingManager;
    private final LockdownManager lockdownManager;

    public ReloadHandler(StagingManager stagingManager, LockdownManager lockdownManager) {
        this.stagingManager = stagingManager;
        this.lockdownManager = lockdownManager;
    }

    public void reload(Context ctx) {
        try {
            List<String> errors = new ArrayList<>();

            // Apply staged changes (backups created automatically by staging manager)
            List<String> applied = stagingManager.applyAndBackup();
            if (applied.isEmpty() && stagingManager.hasPendingChanges()) {
                ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Failed to apply staged changes",
                    "errors", List.of("No files were applied")
                ));
                return;
            }

            // Trigger reload lockdown sequence
            try {
                lockdownManager.reload();
            } catch (Exception e) {
                errors.add("Reload error: " + e.getMessage());
            }

            // Clear staging on success
            stagingManager.clear();

            if (errors.isEmpty()) {
                ctx.json(Map.of(
                    "success", true,
                    "message", "Changes applied. Plugin reloaded successfully.",
                    "errors", List.of()
                ));
            } else {
                ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Reload completed with errors",
                    "errors", errors
                ));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                "success", false,
                "message", "Reload failed: " + e.getMessage(),
                "errors", List.of(e.getMessage())
            ));
        }
    }
}
