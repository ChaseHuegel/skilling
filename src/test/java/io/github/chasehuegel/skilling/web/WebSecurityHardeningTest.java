package io.github.chasehuegel.skilling.web;

import io.github.chasehuegel.skilling.web.auth.AuthRateLimiter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSecurityHardeningTest {

    @Test
    void stateChangingRequestFromDisallowedOriginIsRejected() {
        assertTrue(WebServer.shouldRejectCrossOriginStateChange("https://evil.example", false, "POST"));
        assertTrue(WebServer.shouldRejectCrossOriginStateChange("https://evil.example", false, "PUT"));
        assertTrue(WebServer.shouldRejectCrossOriginStateChange("https://evil.example", false, "DELETE"));
    }

    @Test
    void readOnlyOrAllowedOriginRequestsAreNotRejected() {
        // Read-only methods never mutate state regardless of origin.
        assertFalse(WebServer.shouldRejectCrossOriginStateChange("https://evil.example", false, "GET"));
        assertFalse(WebServer.shouldRejectCrossOriginStateChange("https://evil.example", false, "OPTIONS"));
        // An allowed origin may write.
        assertFalse(WebServer.shouldRejectCrossOriginStateChange("https://allowed.example", true, "POST"));
        // No Origin header (same-origin browser, curl) is never rejected here.
        assertFalse(WebServer.shouldRejectCrossOriginStateChange(null, false, "POST"));
    }

    @Test
    void rateLimiterMapStaysBoundedUnderManyIpFlood() throws Exception {
        AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofMinutes(15), Duration.ofMinutes(15));
        // A flood of distinct IPs, all with windowStart ≈ now, must not grow the
        // attempt map past the hard cap.
        for (int i = 0; i < 10_000; i++) {
            limiter.recordFailure("10." + (i / 65_536) + "." + ((i / 256) % 256) + "." + (i % 256));
        }

        Field attemptsField = AuthRateLimiter.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        int size = ((java.util.Map<?, ?>) attemptsField.get(limiter)).size();
        assertTrue(size <= 4096, "the attempt map must stay bounded, was " + size);
        assertEquals(4096, size, "the map should sit exactly at the hard cap under a flood");
    }
}
