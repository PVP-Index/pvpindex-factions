package com.pvpindex.factions.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Smoke tests for the platform-neutral scheduler abstraction.
 *
 * <p>Tests cover {@link PlatformDetector}, {@link BukkitTaskScheduler} (Spigot/Paper path),
 * and {@link FoliaTaskScheduler} (Folia path) by mocking the underlying Bukkit/Folia APIs
 * via reflection on {@code Bukkit.server}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Scheduler abstraction smoke tests")
class SchedulerSmokeTest {

    @Mock private Plugin plugin;
    @Mock private Server server;

    // -- Bukkit scheduler mocks -----------------------------------------------
    @Mock private BukkitScheduler bukkitScheduler;
    @Mock private BukkitTask bukkitTask;

    // -- Folia scheduler mocks ------------------------------------------------
    @Mock private AsyncScheduler asyncScheduler;
    @Mock private GlobalRegionScheduler globalScheduler;
    @Mock private ScheduledTask scheduledTask;
    @Mock private Player player;
    @Mock private EntityScheduler entityScheduler;

    @BeforeEach
    void injectServer() throws Exception {
        final Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, server);
    }

    @AfterEach
    void clearServer() throws Exception {
        final Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    // =========================================================================
    // PlatformDetector
    // =========================================================================

    @Test
    @DisplayName("PlatformDetector.isFolia is stable across repeated calls")
    void platformDetectorReturnsSameValueOnRepeatedCalls() {
        final boolean first = PlatformDetector.isFolia();
        final boolean second = PlatformDetector.isFolia();
        assertEquals(first, second, "isFolia() must be constant for the lifetime of the JVM");
    }

    // =========================================================================
    // BukkitTaskScheduler – Spigot / Paper path
    // =========================================================================

    @Test
    @DisplayName("BukkitTaskScheduler.runAsync delegates to BukkitScheduler.runTaskAsynchronously")
    void bukkitRunAsyncDelegates() {
        when(server.getScheduler()).thenReturn(bukkitScheduler);
        new BukkitTaskScheduler(plugin).runAsync(mock(Runnable.class));
        verify(bukkitScheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
    }

    @Test
    @DisplayName("BukkitTaskScheduler.runSync delegates to BukkitScheduler.runTask")
    void bukkitRunSyncDelegates() {
        when(server.getScheduler()).thenReturn(bukkitScheduler);
        new BukkitTaskScheduler(plugin).runSync(mock(Runnable.class));
        verify(bukkitScheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @Test
    @DisplayName("BukkitTaskScheduler.runSyncForPlayer delegates to BukkitScheduler.runTask")
    void bukkitRunSyncForPlayerDelegates() {
        when(server.getScheduler()).thenReturn(bukkitScheduler);
        new BukkitTaskScheduler(plugin).runSyncForPlayer(player, mock(Runnable.class));
        verify(bukkitScheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @Test
    @DisplayName("BukkitTaskScheduler.runSyncLater delegates to BukkitScheduler.runTaskLater")
    void bukkitRunSyncLaterDelegates() {
        when(server.getScheduler()).thenReturn(bukkitScheduler);
        new BukkitTaskScheduler(plugin).runSyncLater(mock(Runnable.class), 20L);
        verify(bukkitScheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(20L));
    }

    @Test
    @DisplayName("BukkitTaskScheduler.scheduleAsyncTimer returns CancelableTask backed by BukkitTask")
    void bukkitScheduleAsyncTimerReturnsCancelableThatDelegatesCancel() {
        when(server.getScheduler()).thenReturn(bukkitScheduler);
        when(bukkitScheduler.runTaskTimerAsynchronously(
                eq(plugin), any(Runnable.class), anyLong(), anyLong()))
            .thenReturn(bukkitTask);
        final CancelableTask cancelable =
            new BukkitTaskScheduler(plugin).scheduleAsyncTimer(mock(Runnable.class), 5L, 20L);
        assertNotNull(cancelable);
        cancelable.cancel();
        verify(bukkitTask).cancel();
    }

    // =========================================================================
    // FoliaTaskScheduler – Folia path
    // =========================================================================

    @Test
    @DisplayName("FoliaTaskScheduler.runAsync delegates to AsyncScheduler.runNow")
    void foliaRunAsyncDelegates() {
        when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
        new FoliaTaskScheduler(plugin).runAsync(mock(Runnable.class));
        verify(asyncScheduler).runNow(eq(plugin), any());
    }

    @Test
    @DisplayName("FoliaTaskScheduler.runSync delegates to GlobalRegionScheduler.run")
    void foliaRunSyncDelegates() {
        when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
        new FoliaTaskScheduler(plugin).runSync(mock(Runnable.class));
        verify(globalScheduler).run(eq(plugin), any());
    }

    @Test
    @DisplayName("FoliaTaskScheduler.runSyncForPlayer delegates to EntityScheduler.run")
    void foliaRunSyncForPlayerDelegates() {
        when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
        when(player.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.run(eq(plugin), any(), any())).thenReturn(scheduledTask);
        new FoliaTaskScheduler(plugin).runSyncForPlayer(player, mock(Runnable.class));
        verify(entityScheduler).run(eq(plugin), any(), any());
    }

    @Test
    @DisplayName("FoliaTaskScheduler.runSyncForPlayer falls back to GlobalRegionScheduler when player is gone")
    void foliaRunSyncForPlayerFallsBackToGlobalOnNull() {
        when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
        when(player.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.run(eq(plugin), any(), any())).thenReturn(null);
        new FoliaTaskScheduler(plugin).runSyncForPlayer(player, mock(Runnable.class));
        verify(globalScheduler).run(eq(plugin), any());
    }

    @Test
    @DisplayName("FoliaTaskScheduler.runSyncLater delegates to GlobalRegionScheduler.runDelayed")
    void foliaRunSyncLaterDelegates() {
        when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
        new FoliaTaskScheduler(plugin).runSyncLater(mock(Runnable.class), 20L);
        verify(globalScheduler).runDelayed(eq(plugin), any(), eq(20L));
    }

    @Test
    @DisplayName("FoliaTaskScheduler.scheduleAsyncTimer returns CancelableTask backed by ScheduledTask")
    void foliaScheduleAsyncTimerReturnsCancelableThatDelegatesCancel() {
        when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
        when(asyncScheduler.runAtFixedRate(
                eq(plugin), any(), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS)))
            .thenReturn(scheduledTask);
        final CancelableTask cancelable =
            new FoliaTaskScheduler(plugin).scheduleAsyncTimer(mock(Runnable.class), 5L, 20L);
        assertNotNull(cancelable);
        cancelable.cancel();
        verify(scheduledTask).cancel();
    }
}
