package io.github.chasehuegel.skilling.engine.profile;

import java.util.Map;
import java.util.UUID;

/**
 * Read-only view of a player's skill profile.
 *
 * <p>Addon developers receive this via {@code SkillingAPI.getProfile(UUID)}
 * and can query XP values without accessing internal mutation methods.
 */
public interface PlayerProfileView {

    UUID getPlayerId();

    long getXp(String skillId);

    Map<String, Long> getXpSnapshot();

    boolean isInitialized();
}
