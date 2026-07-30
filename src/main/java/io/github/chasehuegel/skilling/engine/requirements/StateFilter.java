package io.github.chasehuegel.skilling.engine.requirements;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

@FunctionalInterface
public interface StateFilter {
    boolean evaluate(Player player, Event event, String value);
}
