package io.github.chasehuegel.skilling.engine.feedback;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * A Least Recently Used (LRU) cache for player {@link BossBar} instances.
 *
 * <p>Limits the number of active Boss Bars visible on a player's screen
 * to a configurable maximum (default: 2). When a new bar is added and
 * the pool is full, the least recently accessed bar is removed.
 *
 * <p>Uses a {@link LinkedHashMap} with access-order iteration for O(1)
 * LRU eviction. The pool is keyed by {@code <playerUUID>:<skillId>}.
 *
 * <p>YAML configuration key: {@code bossbar.max_active}
 */
public final class BossBarPool {

    private final int maxActive;
    private final Map<String, BossBar> cache;
    private final Map<String, Long> ttlMap;

    /**
     * Constructs a new BossBar pool.
     *
     * @param maxActive maximum number of active Boss Bars per player
     * @param fadeTicks tick duration for the fade-out animation
     */
    public BossBarPool(int maxActive, int fadeTicks) {
        this.maxActive = maxActive;
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
        this.ttlMap = new HashMap<>();
    }

    /**
     * Gets or creates a BossBar for the given player and skill.
     *
     * @param player  the player
     * @param skillId the skill identifier
     * @return the BossBar
     */
    public BossBar getOrCreate(Player player, String skillId) {
        String key = key(player, skillId);

        // Evict LRU entry if at capacity
        if (!cache.containsKey(key) && cache.size() >= maxActive) {
            var eldest = cache.entrySet().iterator().next();
            eldest.getValue().removeAll();
            cache.remove(eldest.getKey());
            ttlMap.remove(eldest.getKey());
        }

        return cache.computeIfAbsent(key, k -> {
            BossBar bar = Bukkit.createBossBar("", org.bukkit.boss.BarColor.WHITE, org.bukkit.boss.BarStyle.SOLID);
            bar.addPlayer(player);
            ttlMap.put(k, 2400L); // 2 minutes at 20 TPS
            return bar;
        });
    }

    /**
     * Returns the BossBar for the given player and skill, or null.
     *
     * @param player  the player
     * @param skillId the skill identifier
     * @return the BossBar, or null
     */
    public BossBar get(Player player, String skillId) {
        String key = key(player, skillId);
        BossBar bar = cache.get(key);
        if (bar != null) {
            ttlMap.put(key, 2400L); // reset TTL on access
        }
        return bar;
    }

    /**
     * Removes and cleans up the BossBar for the given player and skill.
     *
     * @param player  the player
     * @param skillId the skill identifier
     */
    public void remove(Player player, String skillId) {
        String key = key(player, skillId);
        BossBar bar = cache.remove(key);
        ttlMap.remove(key);
        if (bar != null) {
            bar.removeAll();
        }
    }

    /**
     * Removes all BossBars for a given player.
     *
     * @param player the player
     */
    public void removeAll(Player player) {
        String prefix = player.getUniqueId() + ":";
        cache.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(prefix)) {
                ttlMap.remove(e.getKey());
                e.getValue().removeAll();
                return true;
            }
            return false;
        });
    }

    /**
     * Decrements TTL for all bars and removes expired ones.
     * Called every tick from the global update loop.
     */
    public void tickAll() {
        var iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String key = entry.getKey();
            long ttl = ttlMap.getOrDefault(key, 0L) - 1;

            if (ttl <= 0) {
                entry.getValue().removeAll();
                iterator.remove();
                ttlMap.remove(key);
            } else {
                ttlMap.put(key, ttl);
            }
        }
    }

    /**
     * Returns the number of active bars in the pool.
     *
     * @return pool size
     */
    public int size() {
        return cache.size();
    }

    private static String key(Player player, String skillId) {
        return player.getUniqueId() + ":" + skillId;
    }
}