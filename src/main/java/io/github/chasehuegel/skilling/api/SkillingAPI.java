package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * Public API exposed via Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * for addon plugins to interact with the Skilling engine.
 */
public final class SkillingAPI {

    private final Registries registries;

    /**
     * Constructs a new API instance backed by the given registries.
     *
     * @param registries the registries container
     */
    public SkillingAPI(Registries registries) {
        this.registries = registries;
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
     * Asynchronously retrieves a player's profile.
     *
     * @param playerId the player's UUID
     * @return a future yielding the player's profile, or empty if not loaded
     */
    public CompletableFuture<PlayerProfile> getProfile(UUID playerId) {
        return CompletableFuture.completedFuture(null);
    }
}