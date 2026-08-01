package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.Skilling;
import io.javalin.http.Context;

import java.util.Map;

/**
 * REST handler exposing the registered state filter keys for dynamic
 * discovery by the Web GUI.
 *
 * <p><b>GET /api/state-filters</b> — Returns {@code {"stateFilters": [...]}}
 * with the keys currently registered in the {@link Skilling#getStateFilterRegistry()}.
 */
public final class StateFilterHandler {

    private final Skilling plugin;

    public StateFilterHandler(Skilling plugin) {
        this.plugin = plugin;
    }

    /**
     * GET handler: serializes the registered state filter keys to JSON.
     */
    public void list(Context ctx) {
        ctx.json(Map.of("stateFilters", plugin.getStateFilterRegistry().keys()));
    }
}
