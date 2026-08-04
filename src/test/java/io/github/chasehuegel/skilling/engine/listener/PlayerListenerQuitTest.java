package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListenerQuitTest {

    @Test
    void quitClearsDebouncerAndRemovesBossBars() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        FeedbackDebouncer debouncer = new FeedbackDebouncer(500);
        debouncer.tryDebounce(uuid, "ability");
        assertFalse(debouncer.tryDebounce(uuid, "ability"), "pre-condition: debounced");

        BossBarPool bossBarPool = mock(BossBarPool.class);
        PlayerListener listener = new PlayerListener(
                mock(ProfileManager.class), mock(AsyncBatchWorker.class),
                mock(SkillManager.class),
                bossBarPool, debouncer);

        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onPlayerQuit(event);

        verify(bossBarPool).removeAll(player);
        assertTrue(debouncer.tryDebounce(uuid, "ability"),
                "quit must release the player's debounce state");
    }
}
