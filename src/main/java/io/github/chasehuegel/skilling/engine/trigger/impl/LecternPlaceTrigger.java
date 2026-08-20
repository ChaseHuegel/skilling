package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a player places a (signed) book onto a lectern.
 *
 * <p>Paper fires {@code PlayerInsertLecternBookEvent} only when a book is
 * actually inserted into an empty lectern, so ejecting a book from an occupied
 * lectern never fires. This is the "tell your story at home" gesture the bard
 * Lectern ability uses.
 *
 * <p><b>YAML key:</b> {@code lectern_place}
 */
public record LecternPlaceTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "lectern_place"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerInsertLecternBookEvent.class; }
}
