package io.github.chasehuegel.skilling.web.auth;

import io.github.chasehuegel.skilling.web.config.WebConfig;
import java.nio.charset.StandardCharsets;
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
        return config.username().equals(user) && config.password().equals(pass);
    }
}
