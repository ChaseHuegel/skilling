package io.github.chasehuegel.skilling.engine.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player levels up a Skilling skill.
 * This is the event that the {@code level_up} trigger maps to.
 */
public class SkillingLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String skillId;
    private final int newLevel;

    public SkillingLevelUpEvent(Player player, String skillId, int newLevel) {
        this.player = player;
        this.skillId = skillId;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public String getSkillId() {
        return skillId;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
