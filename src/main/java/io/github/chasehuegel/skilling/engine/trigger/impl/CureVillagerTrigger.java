package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * Maps the {@code cure_villager} YAML trigger key to
 * {@link EntityTransformEvent}, granting XP when a zombie villager finishes
 * converting into a villager (reason {@code CURED}).
 *
 * <p>The dispatcher scopes the event to {@code TransformReason.CURED} on a
 * {@link org.bukkit.entity.ZombieVillager} and attributes the cure to the
 * player recorded by {@code ZombieVillager#getConversionPlayer()}.
 */
public final class CureVillagerTrigger implements SkillTrigger {
    @Override
    public String getKey() { return "cure_villager"; }
    @Override
    public Class<? extends Event> getEventClass() { return EntityTransformEvent.class; }
}
