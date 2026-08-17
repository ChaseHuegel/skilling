package io.github.chasehuegel.skilling.engine.trigger.impl;

import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import org.bukkit.event.Event;

/**
 * Trigger fired when a trial vault changes state, for example when a key
 * unlocks it or a reward is dispensed.
 *
 * <p>The dispatcher attributes the event to the triggering player via
 * {@link VaultChangeStateEvent#getPlayer()}, which is null when the state
 * change has no player cause.
 *
 * <p><b>YAML key:</b> {@code vault_change}
 */
public record VaultChangeTrigger() implements SkillTrigger {
    @Override
    public String getKey() { return "vault_change"; }

    @Override
    public Class<? extends Event> getEventClass() { return VaultChangeStateEvent.class; }
}
