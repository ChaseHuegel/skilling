package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Ramps damage by stacking a growing multiplier with each landed hit, holding
 * the ramp for a short window.
 *
 * <p>Each qualifying hit increments the player's stack (capped at
 * {@code max_stacks}) and multiplies that hit's outgoing damage by
 * {@code 1 + stacks * multiplier_step}. The window refreshes on every hit; if a
 * hit lands after the window lapsed, the ramp resets to zero first, so a player
 * who stops fighting must rebuild the bonus. The decay is lazy — the stale ramp
 * is only cleared when the next hit is evaluated — so no scheduled task runs
 * (Pillar V: no constant ticking).
 *
 * <p>YAML key: {@code core:fury}
 * <br>Params: {@code multiplier_step} (damage multiplier added per stack, default 0.1),
 * {@code max_stacks} (stack cap, default 5),
 * {@code window} (seconds before the ramp decays, default 4).
 */
public record FuryMechanic() implements SkillMechanic {

    private static final Map<UUID, State> STACKS = new ConcurrentHashMap<>();
    private static volatile long clockOverrideNanos = 0;

    private record State(int stacks, long windowExpiryNanos) {}

    private static long now() {
        return clockOverrideNanos == 0 ? System.nanoTime() : clockOverrideNanos;
    }

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * clock for the decay-window tests; a zero value uses the real system clock.
     *
     * @param nanos the overridden time in nanoseconds, or 0 to use the system clock
     */
    public static void setClockOverrideNanos(long nanos) {
        clockOverrideNanos = nanos;
    }

    /** Returns the player's current stack count (used by tests). */
    public static int stacks(UUID playerId) {
        State state = STACKS.get(playerId);
        if (state == null || state.windowExpiryNanos() < now()) return 0;
        return state.stacks();
    }

    /** Clears the player's ramp (called on quit). */
    public static void clear(UUID playerId) {
        STACKS.remove(playerId);
    }

    /** Clears every ramp (called on reload/disable). */
    public static void clearAll() {
        STACKS.clear();
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!player.equals(EntityDamageResolver.resolveDamagerPlayer(de))) return false;
        double step = ((Number) params.getOrDefault("multiplier_step", 0.1)).doubleValue();
        int maxStacks = ((Number) params.getOrDefault("max_stacks", 5)).intValue();
        double windowSec = ((Number) params.getOrDefault("window", 4.0)).doubleValue();
        if (step <= 0 || maxStacks <= 0 || windowSec <= 0) return false;

        long now = now();
        long expiry = now + (long) (windowSec * 1_000_000_000L);
        UUID id = player.getUniqueId();

        int stacks = 1;
        State prior = STACKS.get(id);
        if (prior != null && prior.windowExpiryNanos() >= now) {
            stacks = Math.min(prior.stacks() + 1, maxStacks);
        }
        STACKS.put(id, new State(stacks, expiry));

        double multiplier = 1.0 + stacks * step;
        de.setDamage(de.getDamage() * multiplier);
        return true;
    }
}