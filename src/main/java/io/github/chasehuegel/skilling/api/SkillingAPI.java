package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfileView;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
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

    /**
     * Returns the component registry containing all registered mechanics, triggers, and evaluators.
     *
     * @return the registries instance
     */
    public Registries getRegistries() {
        return registries;
    }

    /**
     * Returns the profile manager for loading and caching player data.
     *
     * @return the profile manager instance
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    /**
     * Returns the skill manager that holds all loaded skill definitions.
     *
     * @return the skill manager instance
     */
    public SkillManager getSkillManager() {
        return skillManager;
    }

    /**
     * Returns the builder for constructing skill overview and detail UI inventories.
     *
     * @return the skill menu builder instance
     */
    public SkillMenuBuilder getSkillMenuBuilder() {
        return skillMenuBuilder;
    }

    /**
     * Returns the requirement engine that enforces the check-execute-consume lifecycle for abilities.
     *
     * @return the requirement engine instance
     */
    public RequirementEngine getRequirementEngine() {
        return requirementEngine;
    }

    /**
     * Returns the feedback debouncer for rate-limiting repeated failure messages.
     *
     * @return the feedback debouncer instance
     */
    public FeedbackDebouncer getFeedbackDebouncer() {
        return feedbackDebouncer;
    }

    /**
     * Returns the boss bar pool for displaying XP progress bars to players.
     *
     * <p>All pool methods must be called from the Bukkit main thread; async
     * addon code must hand off via the server scheduler first.
     *
     * @return the boss bar pool instance
     */
    public BossBarPool getBossBarPool() {
        return bossBarPool;
    }

    /**
     * Returns a read-only view of a player's profile from the in-memory cache.
     *
     * <p>Addons receive the immutable {@link PlayerProfileView} contract and can
     * query XP values without access to the engine's mutation methods. Engine
     * internals that need to write use {@code ProfileManager.getProfile} directly.
     *
     * @param playerId the player's UUID
     * @return the profile view, or null if the profile is not loaded
     */
    public PlayerProfileView getProfile(UUID playerId) {
        return profileManager.getProfile(playerId);
    }
}