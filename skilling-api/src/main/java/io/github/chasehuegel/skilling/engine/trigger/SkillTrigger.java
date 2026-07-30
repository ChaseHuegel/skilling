package io.github.chasehuegel.skilling.engine.trigger;

import org.bukkit.event.Event;

/**
 * Maps a YAML trigger key to a Paper event class for XP source and ability routing.
 *
 * <p>Each implementation defines the key used in YAML (e.g., {@code block_break})
 * and the corresponding event class that the engine listens for.
 *
 * <p>Triggers are registered in the TriggerRegistry during {@code onEnable()}
 * and queried by the event dispatcher to route events to matching XP sources
 * and abilities.
 */
public interface SkillTrigger {

    String getKey();

    Class<? extends Event> getEventClass();
}
