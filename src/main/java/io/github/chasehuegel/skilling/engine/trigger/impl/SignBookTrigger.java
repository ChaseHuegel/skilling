package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEditBookEvent;

/**
 * Trigger fired when a player finishes a book-and-quill and signs it into a
 * written book (the "Sign and Close" action).
 *
 * <p>The dispatcher only routes {@code PlayerEditBookEvent} when
 * {@link PlayerEditBookEvent#isSigning()} is true, so merely editing and closing
 * a book-and-quill without signing it does not fire. Signing consumes the
 * book-and-quill, making it a real resource and time cost.
 *
 * <p><b>YAML key:</b> {@code sign_book}
 */
public record SignBookTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "sign_book"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerEditBookEvent.class; }
}
