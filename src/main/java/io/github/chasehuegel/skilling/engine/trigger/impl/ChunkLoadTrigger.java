package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Trigger fired when a player explores newly generated terrain.
 *
 * <p>The listener dispatches this only for {@link ChunkLoadEvent}s whose chunk
 * was generated for the first time ({@code isNewChunk()}), routing it to nearby
 * players, so walking into uncharted land is the exploration action rather than
 * merely loading a chunk from disk.
 *
 * <p><b>YAML key:</b> {@code chunk_load}
 */
public record ChunkLoadTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "chunk_load"; }

    @Override
    public Class<? extends Event> getEventClass() { return ChunkLoadEvent.class; }
}