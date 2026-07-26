package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * Public API exposed via Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * for addon plugins to interact with the Skilling engine.
 */
public final class SkillingAPI {

    private final Registries registries;
    private final ProfileManager profileManager;

    /**
     * Constructs a new API instance backed by the given registries and profile manager.
     *
     * @param registries     the registries container
     * @param profileManager the profile manager
     */
    public SkillingAPI(Registries registries, ProfileManager profileManager) {
        this.registries = registries;
        this.profileManager = profileManager;
    }

    /**
     * Returns the combined registries for mechanics, triggers, and evaluators.
     *
     * @return the registries container
     */
    public Registries getRegistries() {
        return registries;
    }

    /**
     * Returns the profile manager.
     *
     * @return the profile manager
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    /**
     * Asynchronously retrieves a player's profile.
     *
     * @param playerId the player's UUID
     * @return a future yielding the player's profile, or empty if not loaded
     */
    public CompletableFuture<PlayerProfile> getProfile(UUID playerId) {
        PlayerProfile profile = profileManager.getProfile(playerId);
        return CompletableFuture.completedFuture(profile);
    }
}