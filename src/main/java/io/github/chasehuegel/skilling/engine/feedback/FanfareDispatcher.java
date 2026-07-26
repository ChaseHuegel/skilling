package io.github.chasehuegel.skilling.engine.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Map;

/**
 * Parses and dispatches visual/auditory fanfare from YAML feedback nodes.
 *
 * <p>Supports three feedback channels:
 * <ul>
 *   <li><b>Action Bar</b> — text displayed above the hotbar</li>
 *   <li><b>Particles</b> — world particle effects at {@code self} or {@code target} location</li>
 *   <li><b>Sounds</b> — sound effects played at {@code self} or {@code target} location</li>
 * </ul>
 *
 * <p>Uses Adventure's {@link MiniMessage} for text formatting.
 */
public final class FanfareDispatcher {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private FanfareDispatcher() {}

    /**
     * Sends an action bar message to the player.
     *
     * @param player  the target player
     * @param message the message text (MiniMessage format)
     */
    public static void sendActionBar(Player player, String message) {
        if (message == null || message.isBlank()) return;
        player.sendActionBar(MINI_MESSAGE.deserialize(message));
    }

    /**
     * Dispatches all particle effects from a feedback configuration.
     *
     * @param player      the player who activated the ability
     * @param target      the target location (block or entity), may be null
     * @param particles   list of particle configuration maps
     */
    public static void dispatchParticles(Player player, Location target, List<Map<String, Object>> particles) {
        if (particles == null || particles.isEmpty()) return;

        for (var particleConfig : particles) {
            String type = (String) particleConfig.getOrDefault("type", "");
            int count = ((Number) particleConfig.getOrDefault("count", 1)).intValue();
            String targetType = (String) particleConfig.getOrDefault("target", "self");

            Location location = "target".equals(targetType) && target != null
                    ? target : player.getLocation();

            // Parse offset array [x, y, z]
            List<?> offsetRaw = (List<?>) particleConfig.getOrDefault("offset", List.of(0, 0, 0));
            double offsetX = offsetRaw.size() > 0 ? ((Number) offsetRaw.get(0)).doubleValue() : 0;
            double offsetY = offsetRaw.size() > 1 ? ((Number) offsetRaw.get(1)).doubleValue() : 0;
            double offsetZ = offsetRaw.size() > 2 ? ((Number) offsetRaw.get(2)).doubleValue() : 0;
            double speed = ((Number) particleConfig.getOrDefault("speed", 0.0)).doubleValue();

            try {
                var particle = org.bukkit.Particle.valueOf(type);
                player.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
            } catch (IllegalArgumentException ignored) {
                // Unknown particle type - skip silently
            }
        }
    }

    /**
     * Dispatches all sound effects from a feedback configuration.
     *
     * @param player the player who activated the ability
     * @param sounds list of sound configuration maps
     */
    public static void dispatchSounds(Player player, List<Map<String, Object>> sounds) {
        if (sounds == null || sounds.isEmpty()) return;

        for (var soundConfig : sounds) {
            String type = (String) soundConfig.getOrDefault("type", "");
            float volume = ((Number) soundConfig.getOrDefault("volume", 1.0)).floatValue();
            float pitch = ((Number) soundConfig.getOrDefault("pitch", 1.0)).floatValue();
            String target = (String) soundConfig.getOrDefault("target", "self");

            Location location = "target".equals(target) ? player.getLocation() : player.getLocation();

            try {
                Sound sound = Sound.valueOf(type);
                player.getWorld().playSound(location, sound, SoundCategory.PLAYERS, volume, pitch);
            } catch (IllegalArgumentException ignored) {
                // Unknown sound type - skip silently
            }
        }
    }
}