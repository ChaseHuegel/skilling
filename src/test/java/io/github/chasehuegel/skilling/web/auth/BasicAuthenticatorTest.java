package io.github.chasehuegel.skilling.web.auth;

import io.github.chasehuegel.skilling.web.config.WebConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAuthenticatorTest {

    private static final WebConfig CONFIG =
        new WebConfig(true, 8082, "admin", "correct horse battery staple", "0.0.0.0", false, java.util.List.of());

    @Test
    void constantTimeEqualsMatchesEqualStrings() {
        assertTrue(BasicAuthenticator.constantTimeEquals("admin", "admin"));
        assertTrue(BasicAuthenticator.constantTimeEquals("correct horse", "correct horse"));
    }

    @Test
    void constantTimeEqualsRejectsDifferentSameLength() {
        // Differing in the final character is the case an early-exit (non-constant
        // time) comparison would leak by returning faster.
        assertFalse(BasicAuthenticator.constantTimeEquals("password", "passworx"));
        assertFalse(BasicAuthenticator.constantTimeEquals("aaaa", "aaab"));
        assertFalse(BasicAuthenticator.constantTimeEquals("zzzz", "zzzy"));
    }

    @Test
    void constantTimeEqualsRejectsDifferentLengthAndNull() {
        assertFalse(BasicAuthenticator.constantTimeEquals("admin", "administrator"));
        assertFalse(BasicAuthenticator.constantTimeEquals(null, "admin"));
        assertFalse(BasicAuthenticator.constantTimeEquals("admin", null));
    }

    @Test
    void validAcceptsCorrectCredentials() {
        BasicAuthenticator auth = new BasicAuthenticator(CONFIG);
        String header = "Basic " + Base64.getEncoder().encodeToString(
            "admin:correct horse battery staple".getBytes(StandardCharsets.UTF_8));
        assertTrue(auth.valid(header));
    }

    @Test
    void validRejectsWrongPassword() {
        BasicAuthenticator auth = new BasicAuthenticator(CONFIG);
        String header = "Basic " + Base64.getEncoder().encodeToString(
            "admin:wrong".getBytes(StandardCharsets.UTF_8));
        assertFalse(auth.valid(header));
    }

    @Test
    void validRejectsWrongUsername() {
        BasicAuthenticator auth = new BasicAuthenticator(CONFIG);
        String header = "Basic " + Base64.getEncoder().encodeToString(
            "root:correct horse battery staple".getBytes(StandardCharsets.UTF_8));
        assertFalse(auth.valid(header));
    }

    @Test
    void validRejectsMalformedHeaders() {
        BasicAuthenticator auth = new BasicAuthenticator(CONFIG);
        assertFalse(auth.valid(null));
        assertFalse(auth.valid("Bearer abc"));
        assertFalse(auth.valid("Basic !!!not-base64!!!"));
        assertFalse(auth.valid("Basic " + Base64.getEncoder().encodeToString("noColonHere".getBytes())));
    }
}
