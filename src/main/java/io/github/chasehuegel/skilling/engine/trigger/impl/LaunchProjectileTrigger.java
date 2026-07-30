package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class LaunchProjectileTrigger implements SkillTrigger {
    @Override
    public String getKey() { return "launch_projectile"; }
    @Override
    public Class<? extends Event> getEventClass() { return ProjectileLaunchEvent.class; }
}
