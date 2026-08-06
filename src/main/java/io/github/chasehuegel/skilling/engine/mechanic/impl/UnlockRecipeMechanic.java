package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Permanently unlocks a recipe book recipe for the player.
 *
 * <p>The recipe is identified by its namespaced key (for example
 * {@code minecraft:netherite_pickaxe}) and may come from vanilla, a data pack,
 * or another plugin. The unlock writes to the player's persistent recipe book
 * state, so it survives restarts and plugin removal, and the mechanic is a
 * no-op once the recipe is already discovered.
 *
 * <p>Bind it to the {@code level_up} trigger with an {@code unlock_level} for a
 * one-time milestone unlock. The engine also reconciles unlock mechanics on
 * player join and after {@code /skills reload}, so the {@code event} argument
 * can be {@code null}.
 *
 * <p>YAML key: {@code core:unlock_recipe}
 * <br>Params: {@code recipe} (namespaced recipe key)
 */
public record UnlockRecipeMechanic() implements UnlockMechanic {

    private static final Logger LOGGER = Logger.getLogger(UnlockRecipeMechanic.class.getName());

    /** Recipe keys already reported as missing, so a late-registering plugin is warned once, not per execution. */
    private static final Set<String> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Object raw = params.get("recipe");
        if (!(raw instanceof String keyString) || keyString.isBlank()) return false;
        NamespacedKey key = NamespacedKey.fromString(keyString);
        if (key == null) return false;
        // A recipe that is not registered on this server cannot be unlocked.
        // Warn once per key: another plugin may register it after this mechanic
        // was configured (or after a reload), so a missing recipe is a soft
        // no-op rather than a hard failure.
        if (!isRegistered(key)) {
            if (WARNED_MISSING.add(keyString)) {
                LOGGER.warning("core:unlock_recipe: recipe '" + keyString
                        + "' is not registered on this server; the unlock will no-op "
                        + "until a plugin or data pack provides it");
            }
            return false;
        }
        // Idempotency guard: the vanilla recipe book is the persistent grant
        // ledger, so a second execution (later level-up, join, or reload) is a
        // no-op that must not re-fire feedback or consume anything.
        if (player.hasDiscoveredRecipe(key)) return false;
        player.discoverRecipe(key);
        return true;
    }

    private static boolean isRegistered(NamespacedKey key) {
        return Bukkit.getServer().getRecipe(key) != null;
    }
}
