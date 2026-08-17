package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

/**
 * Trigger fired when a player unlocks a new crafting recipe.
 *
 * <p><b>YAML key:</b> {@code recipe_discover}
 */
public record RecipeDiscoverTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "recipe_discover"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerRecipeDiscoverEvent.class; }
}
