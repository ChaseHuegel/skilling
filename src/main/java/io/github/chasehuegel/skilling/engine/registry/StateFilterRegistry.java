package io.github.chasehuegel.skilling.engine.registry;

import io.github.chasehuegel.skilling.engine.requirements.StateFilter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StateFilterRegistry {

    private final Map<String, StateFilter> filters = new ConcurrentHashMap<>();

    public void register(String key, StateFilter filter) {
        filters.put(key.toLowerCase(), filter);
    }

    public StateFilter get(String key) {
        return filters.get(key.toLowerCase());
    }

    public boolean evaluate(String key, Player player, Event event, String value) {
        StateFilter filter = filters.get(key.toLowerCase());
        if (filter == null) return false;
        return filter.evaluate(player, event, value);
    }

    /**
     * Returns all registered state filter keys (lowercased as stored).
     *
     * @return an unmodifiable set of registry keys
     */
    public Set<String> keys() {
        return Collections.unmodifiableSet(filters.keySet());
    }

    public void clear() {
        filters.clear();
    }
}
