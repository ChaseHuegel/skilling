package io.github.chasehuegel.skilling.web;

import io.github.chasehuegel.skilling.web.auth.AuthRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link WebServer#resolveClientIp}: behind a trusted reverse proxy the
 * real client IP (the right-most {@code X-Forwarded-For} entry the proxy appends)
 * is used for rate limiting, so a spoofed header or the proxy's own address can
 * never collapse every client into one lockout bucket.
 */
class WebServerClientIpTest {

    @Test
    void notBehindProxyIgnoresForwardedHeader() {
        // A client cannot spoof its way out of rate limiting when no proxy is trusted.
        assertEquals("10.0.0.1", WebServer.resolveClientIp("10.0.0.1", "203.0.113.9", false));
        assertEquals("10.0.0.1", WebServer.resolveClientIp("10.0.0.1", "203.0.113.9, 198.51.100.7", false));
    }

    @Test
    void behindProxyUsesRightMostForwardedEntry() {
        // The trusted proxy appends the real remote address last.
        assertEquals("198.51.100.7", WebServer.resolveClientIp("10.0.0.1", "203.0.113.9, 198.51.100.7", true));
    }

    @Test
    void behindProxySpoofedLeadingEntriesAreIgnored() {
        // A client-supplied spoofed X-Forwarded-For is overridden by the proxy's append.
        assertEquals("198.51.100.7", WebServer.resolveClientIp("10.0.0.1", "6.6.6.6, 198.51.100.7", true));
    }

    @Test
    void behindProxyFallsBackToSocketIpWhenHeaderMissingOrBlank() {
        assertEquals("10.0.0.1", WebServer.resolveClientIp("10.0.0.1", null, true));
        assertEquals("10.0.0.1", WebServer.resolveClientIp("10.0.0.1", "   ", true));
    }

    @Test
    void forwardedClientFailuresDoNotLockOutOthers() {
        // Mirrors the proxy deployment: the limiter keys on the resolved client IP
        // (right-most X-Forwarded-For entry), so one forwarded client's lockout
        // must not spill onto another client sharing the proxy address.
        AuthRateLimiter limiter = new AuthRateLimiter(2, Duration.ofMinutes(15), Duration.ofMinutes(15));
        String proxyIp = "10.0.0.1";
        String clientA = WebServer.resolveClientIp(proxyIp, "203.0.113.9, 1.2.3.4", true);
        String clientB = WebServer.resolveClientIp(proxyIp, "203.0.113.9, 5.6.7.8", true);

        limiter.recordFailure(clientA);
        limiter.recordFailure(clientA);
        assertTrue(limiter.isBlocked(clientA));

        assertFalse(limiter.isBlocked(clientB));
        assertEquals(2, limiter.failuresRemaining(clientB));
    }
}
