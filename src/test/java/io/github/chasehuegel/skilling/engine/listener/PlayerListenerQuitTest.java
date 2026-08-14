package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
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
                bossBarPool, debouncer, mock(SkillEventListener.class));

        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onPlayerQuit(event);

        verify(bossBarPool).removeAll(player);
        assertTrue(debouncer.tryDebounce(uuid, "ability"),
                "quit must release the player's debounce state");
    }

    @Test
    void quitWarnsAndUnloadsWhenUninitializedDirtyProgressIsDropped() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        // A placeholder profile that earned session XP but never hydrated from the
        // DB (a login-time failure): dirty, but never initialized.
        PlayerProfile profile = new PlayerProfile(uuid);
        profile.setXp("mining", 500);

        ProfileManager profileManager = mock(ProfileManager.class);
        when(profileManager.getProfile(uuid)).thenReturn(profile);
        AsyncBatchWorker worker = mock(AsyncBatchWorker.class);

        PlayerListener listener = new PlayerListener(
                profileManager, worker, mock(SkillManager.class),
                mock(BossBarPool.class), mock(FeedbackDebouncer.class), mock(SkillEventListener.class));

        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        try (MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            Skilling plugin = mock(Skilling.class);
            when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("quit-drop-test"));
            when(Skilling.getInstance()).thenReturn(plugin);
            listener.onPlayerQuit(event);
        }

        verify(profileManager).unloadProfile(uuid, profile);
        verify(worker, never()).flushDirtyProfiles();
    }
}
