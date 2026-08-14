package io.github.chasehuegel.skilling.web.auth;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for the admin web interface.
 *
 * <p>Tracks consecutive failed auth attempts per client IP within a rolling
 * window and blocks that IP for a cooldown period after a threshold. Entries
 * expire naturally; the map is pruned once it grows past a fixed bound so the
 * hot auth path stays O(1) in the common case.
 */
public final class AuthRateLimiter {

    /** Hard ceiling on the attempt map so a many-IP flood cannot grow it without bound. */
    private static final int HARD_CAP = 4096;

    private final int maxFailures;
    private final long windowMillis;
    private final long blockMillis;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    private record AttemptState(int failures, long windowStartMillis, long blockedUntilMillis) {}

    public AuthRateLimiter(int maxFailures, Duration window, Duration block) {
        this.maxFailures = maxFailures;
        this.windowMillis = window.toMillis();
        this.blockMillis = block.toMillis();
    }

    /**
     * Whether the given client is currently locked out.
     *
     * @param ip the client IP
     * @return true if the client is blocked
     */
    public boolean isBlocked(String ip) {
        AttemptState state = attempts.get(ip);
        return state != null && state.blockedUntilMillis() > System.currentTimeMillis();
    }

    /**
     * Number of failures a client may still make before lockout (0 when blocked).
     *
     * @param ip the client IP
     * @return remaining allowed failures
     */
    public int failuresRemaining(String ip) {
        AttemptState state = attempts.get(ip);
        if (state == null) return maxFailures;
        long now = System.currentTimeMillis();
        if (now - state.windowStartMillis() > windowMillis) return maxFailures;
        if (state.blockedUntilMillis() > now) return 0;
        return Math.max(0, maxFailures - state.failures());
    }

    /**
     * Records a failed attempt, locking the client out once the threshold is hit.
     *
     * @param ip the client IP
     */
    public void recordFailure(String ip) {
        long now = System.currentTimeMillis();
        attempts.compute(ip, (key, state) -> {
            if (state == null || now - state.windowStartMillis() > windowMillis) {
                return new AttemptState(1, now, 0);
            }
            int failures = state.failures() + 1;
            long blockedUntil = failures >= maxFailures ? now + blockMillis : 0;
            return new AttemptState(failures, state.windowStartMillis(), blockedUntil);
        });
        prune();
    }

    /**
     * Clears the attempt history for a successful login.
     *
     * @param ip the client IP
     */
    public void recordSuccess(String ip) {
        attempts.remove(ip);
    }

    private void prune() {
        if (attempts.size() <= 1024) return;
        long cutoff = System.currentTimeMillis() - windowMillis - blockMillis;
        attempts.entrySet().removeIf(entry -> entry.getValue().windowStartMillis() < cutoff);
        // A many-IP flood with windowStart ≈ now survives the time-based prune;
        // enforce the hard cap by evicting the oldest entries so the map stays
        // bounded. Sorted only when over the cap (the extreme case).
        if (attempts.size() > HARD_CAP) {
            int excess = attempts.size() - HARD_CAP;
            attempts.entrySet().stream()
                    .sorted(java.util.Comparator.comparingLong(e -> e.getValue().windowStartMillis()))
                    .limit(excess)
                    .forEach(e -> attempts.remove(e.getKey(), e.getValue()));
        }
    }
}
