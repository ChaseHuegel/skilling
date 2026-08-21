package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Trigger fired when a player carries a map into freshly generated terrain.
 *
 * <p>The listener dispatches this only for {@link ChunkLoadEvent}s whose chunk
 * was generated for the first time ({@code isNewChunk()}), routed to players
 * near the new chunk who are holding a map element (an empty {@code map} or a
 * {@code filled_map}) in either hand. Holding the map is what ties the act to
 * cartography: "fill your map as you strike out into uncharted land" is the
 * rewarded action rather than raw exploration.
 *
 * <p>Dispatch shares the {@code chunk_load} throttle window, so the rate of
 * map-fill awards is bounded alongside the exploration dispatch.
 *
 * <p><b>YAML key:</b> {@code map_explore}
 */
public record MapExploreTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "map_explore"; }

    @Override
    public Class<? extends Event> getEventClass() { return ChunkLoadEvent.class; }
}