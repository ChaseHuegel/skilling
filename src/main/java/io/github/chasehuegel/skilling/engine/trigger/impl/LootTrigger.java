package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;

/**
 * Trigger fired when loot is generated in the world, for example when a chest
 * or a block such as a trial-chamber container fills with loot.
 *
 * <p>The dispatcher routes this to nearby players of the loot location, because
 * {@link LootGenerateEvent} carries no single owning player; two players looting
 * the same container both count, which suits group play.
 *
 * <p><b>YAML key:</b> {@code loot}
 */
public record LootTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "loot"; }

    @Override
    public Class<? extends Event> getEventClass() { return LootGenerateEvent.class; }
}
