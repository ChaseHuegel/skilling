package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReloadHandler {

    private static final Logger LOGGER = Logger.getLogger(ReloadHandler.class.getName());

    /** How long to wait for the reload to start on the main thread before giving up. */
    private static final int RELOAD_START_TIMEOUT_SECONDS = 3;
    /** How long to wait for the reload sequence (flush + rebuild) to finish. */
    private static final int RELOAD_TIMEOUT_SECONDS = 10;

    private final Skilling plugin;
    private final StagingManager stagingManager;
    private final LockdownManager lockdownManager;

    public ReloadHandler(Skilling plugin, StagingManager stagingManager, LockdownManager lockdownManager) {
        this.plugin = plugin;
        this.stagingManager = stagingManager;
        this.lockdownManager = lockdownManager;
    }

    public void reload(Context ctx) {
        try {
            List<String> errors = new ArrayList<>();

            // Check for conflicts first
            List<String> conflicts = stagingManager.checkConflicts();
            if (!conflicts.isEmpty()) {
                ctx.status(409).json(Map.of(
                    "success", false,
                    "message", "Conflict detected: live files modified since staging",
                    "errors", conflicts
                ));
                return;
            }

            // Apply staged changes (backups created automatically by staging manager).
            // A mid-apply copy failure throws here, leaving staging intact for retry.
            List<String> applied = stagingManager.applyAndBackup();
            if (applied.isEmpty() && stagingManager.hasPendingChanges()) {
                ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Failed to apply staged changes",
                    "errors", List.of("No files were applied")
                ));
                return;
            }

            // Trigger reload lockdown sequence on the main thread. The reload itself
            // is asynchronous (DB flush + rebuild), so both the main-thread handoff
            // and the completion wait are bounded — a stalled main thread surfaces
            // a timed-out error instead of hanging the Jetty worker indefinitely.
            try {
                java.util.concurrent.Future<java.util.concurrent.CompletableFuture<Void>> started =
                        org.bukkit.Bukkit.getScheduler().callSyncMethod(
                            plugin,
                            (java.util.concurrent.Callable<java.util.concurrent.CompletableFuture<Void>>)
                                    lockdownManager::reloadAsync
                        );
                var reloadDone = started.get(RELOAD_START_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                reloadDone.get(RELOAD_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                LOGGER.log(Level.WARNING, "Reload timed out", e);
                errors.add("Reload timed out");
            } catch (Exception e) {
                // Log the full detail server-side; the response body only gets a
                // generic marker so lock-down errors never leak internal text.
                LOGGER.log(Level.WARNING, "Reload lockdown step failed", e);
                errors.add("Reload lockdown failed");
            }

            if (errors.isEmpty()) {
                // Only discard pending edits once the reload fully succeeded, so a
                // failed apply can be retried from the preserved staging.
                stagingManager.clear();
                ctx.json(Map.of(
                    "success", true,
                    "message", "Changes applied. Plugin reloaded successfully.",
                    "errors", List.of()
                ));
            } else {
                ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Reload completed with errors; staging preserved for retry",
                    "errors", errors
                ));
            }
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Reload failed", e);
        }
    }
}
