package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Trigger fired when a player's item takes durability damage.
 *
 * <p><b>YAML key:</b> {@code item_damage}
 */
public record ItemDamageTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "item_damage"; }

    @Override
    public Class<? extends Event> getEventClass() { return PlayerItemDamageEvent.class; }
}
