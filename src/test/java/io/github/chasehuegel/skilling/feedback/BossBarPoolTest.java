package io.github.chasehuegel.skilling.feedback;

import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BossBarPoolTest {

    private MockedStatic<Bukkit> bukkit;
    private BossBar mockBar;
    private Player player;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        mockBar = mock(BossBar.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        bukkit.when(() -> Bukkit.createBossBar(anyString(), any(), any())).thenReturn(mockBar);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void getOrCreateReturnsBossBar() {
        var pool = new BossBarPool(5, 20);
        BossBar bar = pool.getOrCreate(player, "mining");
        assertNotNull(bar);
        assertSame(mockBar, bar);
    }

    @Test
    void sizeIncreasesAfterGetOrCreate() {
        var pool = new BossBarPool(5, 20);
        assertEquals(0, pool.size());
        pool.getOrCreate(player, "mining");
        assertEquals(1, pool.size());
    }

    @Test
    void removeRemovesBar() {
        var pool = new BossBarPool(5, 20);
        pool.getOrCreate(player, "mining");
        assertEquals(1, pool.size());
        pool.remove(player, "mining");
        assertEquals(0, pool.size());
    }

    @Test
    void tickAllDecrementsTtl() {
        var pool = new BossBarPool(5, 2);
        pool.getOrCreate(player, "mining");
        assertEquals(1, pool.size());

        pool.tickAll();
        assertEquals(1, pool.size());

        pool.tickAll();
        assertEquals(0, pool.size());
    }

    @Test
    void getReturnsBarAndResetsTtl() {
        var pool = new BossBarPool(5, 20);
        pool.getOrCreate(player, "mining");
        BossBar bar = pool.get(player, "mining");
        assertSame(mockBar, bar);
    }

    @Test
    void removeAllRemovesAllBarsForPlayer() {
        var pool = new BossBarPool(5, 20);
        pool.getOrCreate(player, "mining");
        pool.getOrCreate(player, "woodcutting");
        assertEquals(2, pool.size());

        pool.removeAll(player);
        assertEquals(0, pool.size());
    }
}
