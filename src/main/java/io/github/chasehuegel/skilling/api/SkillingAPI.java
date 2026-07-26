package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * Public API exposed via Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * for addon plugins to interact with the Skilling engine.
 */
public final class SkillingAPI {

    private final Registries registries;
    private final ProfileManager profileManager;
    private final SkillManager skillManager;
    private final SkillMenuBuilder skillMenuBuilder;
    private final RequirementEngine requirementEngine;
    private final FeedbackDebouncer feedbackDebouncer;
    private final BossBarPool bossBarPool;

    public SkillingAPI(Registries registries, ProfileManager profileManager,
                       SkillManager skillManager, SkillMenuBuilder skillMenuBuilder,
                       RequirementEngine requirementEngine,
                       FeedbackDebouncer feedbackDebouncer, BossBarPool bossBarPool) {
        this.registries = registries;
        this.profileManager = profileManager;
        this.skillManager = skillManager;
        this.skillMenuBuilder = skillMenuBuilder;
        this.requirementEngine = requirementEngine;
        this.feedbackDebouncer = feedbackDebouncer;
        this.bossBarPool = bossBarPool;
    }

    public Registries getRegistries() {
        return registries;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public SkillMenuBuilder getSkillMenuBuilder() {
        return skillMenuBuilder;
    }

    public RequirementEngine getRequirementEngine() {
        return requirementEngine;
    }

    public FeedbackDebouncer getFeedbackDebouncer() {
        return feedbackDebouncer;
    }

    public BossBarPool getBossBarPool() {
        return bossBarPool;
    }

    public CompletableFuture<PlayerProfile> getProfile(UUID playerId) {
        PlayerProfile profile = profileManager.getProfile(playerId);
        return CompletableFuture.completedFuture(profile);
    }
}