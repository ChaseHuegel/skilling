package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:sneak_effect}: applies a potion effect on sneak and that
 * {@link SneakEffectMechanic#strip} removes it on release, so the effect never
 * lingers after the player stops sneaking.
 */
class SneakEffectMechanicTest {

    private static final UUID PLAYER_ID = UUID.fromString("aaaaaaaa-1111-2222-3333-444444444444");

    @Test
    void appliesEffectOnSneak() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        var type = mock(PotionEffectType.class);

        SneakEffectMechanic.apply(player, type, 0, 5);
        verify(player).addPotionEffect(org.mockito.ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void stripRemovesAppliedEffectOnRelease() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        var type = mock(PotionEffectType.class);

        SneakEffectMechanic.apply(player, type, 0, 5);
        SneakEffectMechanic.strip(player);
        verify(player).removePotionEffect(type);
    }
}
