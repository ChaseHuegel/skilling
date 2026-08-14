package io.github.chasehuegel.skilling.web.auth;

import io.github.chasehuegel.skilling.web.config.WebConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class BasicAuthenticator {

    private final WebConfig config;

    public BasicAuthenticator(WebConfig config) {
        this.config = config;
    }

    public boolean valid(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false;
        String base64 = authHeader.substring(6);
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) return false;
        String user = decoded.substring(0, colon);
        String pass = decoded.substring(colon + 1);
        // Compare username and password as one concatenated value so a username
        // mismatch cannot short-circuit and leak the username via timing; both
        // fields go through a single constant-time comparison.
        return constantTimeEquals(config.username() + "\u0000" + config.password(),
                user + "\u0000" + pass);
    }

    /**
     * Constant-time string comparison via {@link MessageDigest#isEqual}, so the
     * time to reject a wrong credential does not depend on how many leading
     * characters matched. Equal-length inputs take constant time; the length
     * of the combined value is not treated as secret.
     *
     * @param expected the expected value
     * @param actual   the value supplied by the client
     * @return true if the values are equal
     */
    static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
