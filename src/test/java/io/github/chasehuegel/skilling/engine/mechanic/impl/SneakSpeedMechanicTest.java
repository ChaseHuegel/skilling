package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:sneak_speed}: applies a movement-speed modifier on sneak
 * and that {@link SneakSpeedMechanic#strip} removes it on release, so the bonus
 * never lingers after the player stops sneaking.
 */
class SneakSpeedMechanicTest {

    private static final UUID PLAYER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String MODIFIER = "6f3c1d2e-9a4b-4c5d-8e1f-0a2b3c4d5e6f";

    private static AttributeInstance attribute(Player player) {
        AttributeInstance inst = mock(AttributeInstance.class);
        when(inst.getBaseValue()).thenReturn(0.1);
        when(inst.getModifier(any(java.util.UUID.class))).thenReturn(mock(AttributeModifier.class));
        when(player.getAttribute(Attribute.MOVEMENT_SPEED)).thenReturn(inst);
        return inst;
    }

    @Test
    void multiplierAtOrBelowOneIsNoOp() {
        var player = mock(Player.class);
        attribute(player);
        assertFalse(new SneakSpeedMechanic().execute(player, Map.of("multiplier", 1.0), null));
        assertFalse(new SneakSpeedMechanic().execute(player, Map.of(), null));
    }

    @Test
    void noAttributeIsNoOp() {
        var player = mock(Player.class);
        when(player.getAttribute(Attribute.MOVEMENT_SPEED)).thenReturn(null);
        assertFalse(new SneakSpeedMechanic().execute(player, Map.of("multiplier", 1.5), null));
    }

    @Test
    void appliesSpeedModifierOnSneak() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        AttributeInstance inst = attribute(player);

        assertTrue(new SneakSpeedMechanic().execute(player,
                Map.of("multiplier", 1.5, "uuid", MODIFIER), null));
        verify(inst).addTransientModifier(any(AttributeModifier.class));
    }

    @Test
    void stripRemovesAppliedModifierOnRelease() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        AttributeInstance inst = attribute(player);

        new SneakSpeedMechanic().execute(player, Map.of("multiplier", 1.5, "uuid", MODIFIER), null);
        SneakSpeedMechanic.strip(player);
        verify(inst, atLeastOnce()).removeModifier(any(AttributeModifier.class));
    }
}
