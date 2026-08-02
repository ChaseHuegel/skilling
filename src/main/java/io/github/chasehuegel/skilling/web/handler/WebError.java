package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared HTTP error responses that never leak internal exception details.
 *
 * <p>500 bodies carry a generic message; full details are logged server-side.
 * 400 responses carry short, client-safe validation messages.
 */
final class WebError {

    private WebError() {}

    /**
     * Parses the JSON request body. Javalin's {@code bodyAsClass} throws
     * {@link JsonProcessingException} on malformed JSON at runtime without
     * declaring it (Kotlin interop); this wrapper declares it so callers can
     * catch it and return 400.
     */
    static <T> T parseBody(Context ctx, Class<T> type) throws JsonProcessingException {
        return ctx.bodyAsClass(type);
    }

    /** Returns a 500 with a generic message and logs the full detail server-side. */
    static void internal(Context ctx, Logger logger, String what, Exception e) {
        logger.log(Level.WARNING, what, e);
        ctx.status(500).json(Map.of("status", "error", "message", "Internal server error"));
    }

    /** Returns a 400 with a client-safe message. */
    static void badRequest(Context ctx, String message) {
        ctx.status(400).json(Map.of("status", "error", "message", message));
    }

    /** Returns a 400 for an unparsable or wrong-shaped JSON body. */
    static void malformedJson(Context ctx) {
        badRequest(ctx, "Malformed JSON body");
    }
}
