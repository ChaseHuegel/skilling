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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private Player newPlayer() {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        return p;
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
    void perPlayerLruKeepsBarsPerPlayerScreen() {
        var pool = new BossBarPool(2, 20);
        pool.getOrCreate(newPlayer(), "mining");
        pool.getOrCreate(newPlayer(), "woodcutting");
        pool.getOrCreate(newPlayer(), "mining");
        pool.getOrCreate(newPlayer(), "woodcutting");
        pool.getOrCreate(newPlayer(), "mining");
        pool.getOrCreate(newPlayer(), "woodcutting");
        assertEquals(6, pool.size(),
                "max_active must bound each player's screen, not the whole server");
    }

    @Test
    void playerExceedingCapEvictsOwnLruBarOnly() {
        var pool = new BossBarPool(2, 20);
        pool.getOrCreate(player, "mining");
        pool.getOrCreate(player, "woodcutting");
        assertEquals(2, pool.size());
        pool.getOrCreate(player, "farming");
        assertEquals(2, pool.size(), "the player's own LRU bar is evicted to honor the cap");
    }

    @Test
    void concurrentGetOrCreateForSameKeyYieldsOneActiveBar() throws Exception {
        var pool = new BossBarPool(5, 20);
        pool.getOrCreate(player, "mining"); // seed the bar on the main thread

        ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            Future<BossBar> f1 = exec.submit(() -> pool.getOrCreate(player, "mining"));
            Future<BossBar> f2 = exec.submit(() -> pool.getOrCreate(player, "mining"));
            assertSame(mockBar, f1.get(5, TimeUnit.SECONDS));
            assertSame(mockBar, f2.get(5, TimeUnit.SECONDS));
        } finally {
            exec.shutdown();
        }
        assertEquals(1, pool.size(), "one active bar for the same key, never an orphaned duplicate");
    }

    @Test
    void tickAllUnderConcurrentAccessDoesNotThrow() throws Exception {
        var pool = new BossBarPool(5, 2);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        // The writer only removes (no Bukkit call); the main thread creates bars.
        Thread writer = new Thread(() -> {
            try {
                while (running.get()) {
                    pool.remove(player, "mining");
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        writer.start();

        try {
            for (int i = 0; i < 200; i++) {
                pool.getOrCreate(player, "mining");
                pool.tickAll();
            }
        } finally {
            running.set(false);
            writer.join(5000);
        }

        assertNull(failure.get(), "tickAll must not throw CME under concurrent access");
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

    @Test
    void removeAllHidesAndClearsEveryBar() {
        var pool = new BossBarPool(5, 20);
        pool.getOrCreate(player, "mining");
        pool.getOrCreate(player, "woodcutting");
        assertEquals(2, pool.size());

        pool.removeAll();
        assertEquals(0, pool.size());
        // Every pooled bar is hidden so none survives a plugin disable.
        verify(mockBar, times(2)).setVisible(false);
    }

    @Test
    void setMaxActiveUpdatesPerPlayerCapAtRuntime() {
        var pool = new BossBarPool(2, 20);
        pool.getOrCreate(player, "a");
        pool.getOrCreate(player, "b");
        assertEquals(2, pool.size());

        pool.setMaxActive(5);
        pool.getOrCreate(player, "c");
        pool.getOrCreate(player, "d");
        pool.getOrCreate(player, "e");
        assertEquals(5, pool.size(), "raising the cap at runtime must allow more bars per player");
    }
}
