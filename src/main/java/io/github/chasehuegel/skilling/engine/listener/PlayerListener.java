package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class PlayerListener implements Listener {

    private final ProfileManager profileManager;
    private final AsyncBatchWorker asyncBatchWorker;
    private final RequirementEngine requirementEngine;
    private final SkillManager skillManager;
    private final BossBarPool bossBarPool;
    private final FeedbackDebouncer feedbackDebouncer;

    public PlayerListener(ProfileManager profileManager, AsyncBatchWorker asyncBatchWorker,
                          RequirementEngine requirementEngine, SkillManager skillManager,
                          BossBarPool bossBarPool, FeedbackDebouncer feedbackDebouncer) {
        this.profileManager = profileManager;
        this.asyncBatchWorker = asyncBatchWorker;
        this.requirementEngine = requirementEngine;
        this.skillManager = skillManager;
        this.bossBarPool = bossBarPool;
        this.feedbackDebouncer = feedbackDebouncer;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        profileManager.loadProfile(event.getUniqueId()).join();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Skilling.getInstance().isReloading()) return;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) return;
        for (String skillId : profile.pendingFanfareSkills()) {
            SkillDefinition skill = skillManager.getSkill(skillId);
            if (skill == null) continue;
            int level = skill.getLevelForXp(profile.getXp(skillId));
            LevelUpDispatcher.broadcastLevelUp(player, skill, level, Skilling.getInstance(), bossBarPool);
            profile.consumePendingFanfare(skillId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        requirementEngine.clearCooldowns(player);
        // Release per-player state so memory does not grow unboundedly with unique
        // players; boss bars are hidden by removeAll. Runs on the main thread so it
        // never races the dispatch path.
        feedbackDebouncer.clear(player.getUniqueId());
        bossBarPool.removeAll(player);
        io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic.clear(player.getUniqueId());
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile != null && profile.isDirty()) {
            // Capture the session generation now; if the player rejoins while the
            // flush is in flight, the reconnect bumps the generation and the
            // completion must not evict the live profile.
            long generationAtQuit = profileManager.sessionGeneration(player.getUniqueId());
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
                // Only remove the exact instance from the same session; if the
                // player reconnected, the generation no longer matches and the
                // live profile is left in place.
                profileManager.unloadProfile(player.getUniqueId(), profile, generationAtQuit);
            });
        } else if (profile != null) {
            profileManager.unloadProfile(player.getUniqueId(), profile);
        }
    }
}
