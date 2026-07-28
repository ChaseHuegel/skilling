package io.github.chasehuegel.skilling.engine.feedback;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final int fadeTicks;
    private final Map<String, BossBar> cache;
    private final Map<String, Long> ttlMap;

    /**
     * Constructs a new BossBar pool.
     *
     * @param maxActive maximum number of active Boss Bars per player
     * @param fadeTicks tick duration for the fade-out animation (TTL)
     */
    public BossBarPool(int maxActive, int fadeTicks) {
        this.maxActive = maxActive;
        this.fadeTicks = fadeTicks;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true));
        this.ttlMap = new ConcurrentHashMap<>();
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

        synchronized (cache) {
            if (!cache.containsKey(key) && cache.size() >= maxActive) {
                var eldest = cache.entrySet().iterator().next();
                hideBar(eldest.getValue());
                cache.remove(eldest.getKey());
                ttlMap.remove(eldest.getKey());
            }
        }

        BossBar bar = cache.get(key);
        if (bar != null) {
            ttlMap.put(key, (long) fadeTicks);
            return bar;
        }

        bar = Bukkit.createBossBar("", org.bukkit.boss.BarColor.WHITE, org.bukkit.boss.BarStyle.SOLID);
        bar.addPlayer(player);
        cache.put(key, bar);
        ttlMap.put(key, (long) fadeTicks);
        return bar;
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
            ttlMap.put(key, (long) fadeTicks); // reset TTL on access
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
            hideBar(bar);
        }
    }

    /**
     * Removes all BossBars for a given player.
     *
     * @param player the player
     */
    public void removeAll(Player player) {
        String prefix = player.getUniqueId() + ":";
        synchronized (cache) {
            cache.entrySet().removeIf(e -> {
                if (e.getKey().startsWith(prefix)) {
                    ttlMap.remove(e.getKey());
                    hideBar(e.getValue());
                    return true;
                }
                return false;
            });
        }
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
                hideBar(entry.getValue());
                iterator.remove();
                ttlMap.remove(key);
            } else {
                ttlMap.put(key, ttl);
            }
        }
    }

    private static void hideBar(BossBar bar) {
        bar.setVisible(false);
        bar.removeAll();
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