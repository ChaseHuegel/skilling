package io.github.chasehuegel.skilling.web.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {

    @Test
    void blocksClientAfterThresholdFailures() {
        AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofMinutes(15), Duration.ofMinutes(15));

        assertFalse(limiter.isBlocked("1.2.3.4"));
        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        assertFalse(limiter.isBlocked("1.2.3.4"));
        assertEquals(1, limiter.failuresRemaining("1.2.3.4"));

        limiter.recordFailure("1.2.3.4");
        assertTrue(limiter.isBlocked("1.2.3.4"));
        assertEquals(0, limiter.failuresRemaining("1.2.3.4"));
    }

    @Test
    void successfulAuthResetsCounter() {
        AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofMinutes(15), Duration.ofMinutes(15));

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordSuccess("1.2.3.4");

        assertEquals(3, limiter.failuresRemaining("1.2.3.4"));
        assertFalse(limiter.isBlocked("1.2.3.4"));
    }

    @Test
    void windowExpiryClearsFailures() throws Exception {
        AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofMillis(50), Duration.ofMillis(50));

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        assertTrue(limiter.isBlocked("1.2.3.4"));

        Thread.sleep(80);
        assertFalse(limiter.isBlocked("1.2.3.4"));
        assertEquals(3, limiter.failuresRemaining("1.2.3.4"));
    }

    @Test
    void blockCooldownExpires() throws Exception {
        AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofMinutes(15), Duration.ofMillis(60));

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        assertTrue(limiter.isBlocked("1.2.3.4"));

        Thread.sleep(100);
        assertFalse(limiter.isBlocked("1.2.3.4"));
    }

    @Test
    void clientsAreTrackedIndependently() {
        AuthRateLimiter limiter = new AuthRateLimiter(2, Duration.ofMinutes(15), Duration.ofMinutes(15));

        limiter.recordFailure("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        assertTrue(limiter.isBlocked("1.2.3.4"));

        assertFalse(limiter.isBlocked("5.6.7.8"));
        assertEquals(2, limiter.failuresRemaining("5.6.7.8"));
    }
}
