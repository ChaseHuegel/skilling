package io.github.chasehuegel.skilling.engine.profile;

import java.util.Map;
import java.util.UUID;

/**
 * Read-only view of a player's skill profile.
 *
 * <p>Addon developers receive this via {@code SkillingAPI.getProfile(UUID)}
 * and can query XP values without accessing internal mutation methods. The
 * concrete engine profile is never exposed through the public API.
 */
public interface PlayerProfileView {

    /**
     * Returns the player's UUID.
     *
     * @return the player's UUID
     */
    UUID getPlayerId();

    /**
     * Returns the raw XP for the given skill, or 0 if the skill is not tracked.
     *
     * @param skillId the skill identifier
     * @return the raw XP
     */
    long getXp(String skillId);

    /**
     * Returns a snapshot copy of the XP map, so callers never see a partially
     * updated view if the live profile is concurrently modified.
     *
     * @return a copy of the skill → XP map
     */
    Map<String, Long> getXpSnapshot();

    /**
     * Whether the profile has been hydrated from the database.
     *
     * @return true once the profile is fully initialized
     */
    boolean isInitialized();
}
