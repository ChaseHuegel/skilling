package io.github.chasehuegel.skilling.engine.profile;

/**
 * Per-player logging preferences controlling which notification types
 * appear in chat.
 *
 * @param logXp        chat message on each XP gain (default false)
 * @param logLevels    override level-up notification (default true)
 * @param logUnlocks   override unlock notification (default true)
 * @param logAbilities override ability activation message (default true)
 */
public record PlayerPreferences(
        boolean logXp,
        boolean logLevels,
        boolean logUnlocks,
        boolean logAbilities
) {
    public static final PlayerPreferences DEFAULTS = new PlayerPreferences(false, true, true, true);
}
