package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class PlayerListener implements Listener {

    private final ProfileManager profileManager;
    private final AsyncBatchWorker asyncBatchWorker;
    private final RequirementEngine requirementEngine;

    public PlayerListener(ProfileManager profileManager, AsyncBatchWorker asyncBatchWorker,
                          RequirementEngine requirementEngine) {
        this.profileManager = profileManager;
        this.asyncBatchWorker = asyncBatchWorker;
        this.requirementEngine = requirementEngine;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        profileManager.loadProfile(event.getUniqueId()).join();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        requirementEngine.clearCooldowns(player);
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile != null && profile.isDirty()) {
            CompletableFuture.runAsync(() -> {
                asyncBatchWorker.flushDirtyProfiles();
            }).whenComplete((v, ex) -> {
                if (ex != null) {
                    Skilling.getInstance().getLogger().log(Level.WARNING, "Failed to flush dirty profiles on quit for " + player.getName(), ex);
                }
                // Check if profile was modified between async flush and now
                if (profile.isDirty()) {
                    asyncBatchWorker.flushDirtyProfiles();
                }
                profileManager.unloadProfile(player.getUniqueId());
            });
        } else {
            profileManager.unloadProfile(player.getUniqueId());
        }
    }
}
