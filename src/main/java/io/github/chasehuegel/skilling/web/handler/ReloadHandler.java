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
    private final Runnable skillIndexInvalidator;
    /** Single-flight guard: concurrent reloads must not interleave the lockdown sequence. */
    private final java.util.concurrent.atomic.AtomicBoolean reloadInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);

    public ReloadHandler(Skilling plugin, StagingManager stagingManager, LockdownManager lockdownManager) {
        this(plugin, stagingManager, lockdownManager, () -> {});
    }

    public ReloadHandler(Skilling plugin, StagingManager stagingManager, LockdownManager lockdownManager,
                         Runnable skillIndexInvalidator) {
        this.plugin = plugin;
        this.stagingManager = stagingManager;
        this.lockdownManager = lockdownManager;
        this.skillIndexInvalidator = skillIndexInvalidator;
    }

    public void reload(Context ctx) {
        if (!reloadInProgress.compareAndSet(false, true)) {
            ctx.status(409).json(Map.of(
                "success", false,
                "message", "A reload is already in progress",
                "errors", List.of("Concurrent reload rejected")
            ));
            return;
        }
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
                // applyAndBackup returns empty only when its locked re-check found a
                // conflict that appeared after the initial check; that is a client
                // conflict (409), not a server error (500).
                ctx.status(409).json(Map.of(
                    "success", false,
                    "message", "Conflict detected: live files modified since staging",
                    "errors", stagingManager.checkConflicts()
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
                // Discard only the applied entries once the reload fully
                // succeeded, so an edit staged while the apply/reload window was
                // in flight is preserved instead of being wiped by a full clear.
                stagingManager.clearApplied(applied);
                // The live skills changed; drop the id→path cache so lookups
                // re-resolve against the new tree.
                skillIndexInvalidator.run();
                ctx.json(Map.of(
                    "success", true,
                    "message", "Changes applied. Plugin reloaded successfully.",
                    "errors", List.of()
                ));
            } else {
                // The live files were already copied into place; restore them from
                // the apply's backup so the engine's old state and the live tree
                // stay consistent, then preserve staging for a retry.
                stagingManager.restoreApplied(applied);
                ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Reload completed with errors; live files restored and staging preserved for retry",
                    "errors", errors
                ));
            }
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Reload failed", e);
        } finally {
            reloadInProgress.set(false);
        }
    }
}
