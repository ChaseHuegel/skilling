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
 * to a configurable maximum (default: 2). When a player gains a new bar past
 * their own cap, that player's least recently accessed bar is removed, so bars
 * never flicker out because of <em>other</em> players' XP gains.
 *
 * <p>Uses an access-ordered {@link LinkedHashMap}; the whole get-or-create
 * (including eviction and creation) runs under the pool lock so concurrent
 * callers (including async addons) never orphan a visible bar.
 *
 * <p>YAML configuration keys: {@code bossbar.max_active}, {@code bossbar.fade_ticks}.
 */
public final class BossBarPool {

    private volatile int maxActive;
    private volatile int fadeTicks;
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
     * Sets the per-player cap (from config) at runtime.
     *
     * @param maxActive maximum active bars per player
     */
    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    /**
     * Sets the TTL in ticks (from config) at runtime.
     *
     * @param fadeTicks tick duration for the fade-out animation
     */
    public void setFadeTicks(int fadeTicks) {
        this.fadeTicks = fadeTicks;
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
        String prefix = player.getUniqueId() + ":";
        synchronized (cache) {
            // The full get-or-create (eviction + creation + insertion) is atomic.
            BossBar bar = cache.get(key);
            if (bar != null) {
                ttlMap.put(key, (long) fadeTicks);
                return bar;
            }
            // Evict only this player's least-recently-used bar(s) down to the cap.
            while (countForPrefix(prefix) >= maxActive) {
                String eldest = null;
                for (String k : cache.keySet()) {
                    if (k.startsWith(prefix)) {
                        eldest = k;
                        break;
                    }
                }
                if (eldest == null) break;
                BossBar evicted = cache.remove(eldest);
                ttlMap.remove(eldest);
                hideBar(evicted);
            }
            bar = Bukkit.createBossBar("", org.bukkit.boss.BarColor.WHITE, org.bukkit.boss.BarStyle.SOLID);
            bar.addPlayer(player);
            cache.put(key, bar);
            ttlMap.put(key, (long) fadeTicks);
            return bar;
        }
    }

    /**
     * Removes and cleans up the BossBar for the given player and skill.
     *
     * @param player  the player
     * @param skillId the skill identifier
     */
    public void remove(Player player, String skillId) {
        String key = key(player, skillId);
        synchronized (cache) {
            BossBar bar = cache.remove(key);
            ttlMap.remove(key);
            if (bar != null) {
                hideBar(bar);
            }
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
        synchronized (cache) {
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
    }

    private int countForPrefix(String prefix) {
        int count = 0;
        for (String k : cache.keySet()) {
            if (k.startsWith(prefix)) count++;
        }
        return count;
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
