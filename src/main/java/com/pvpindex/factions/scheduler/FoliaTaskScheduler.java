package com.pvpindex.factions.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * {@link TaskScheduler} implementation for Folia.
 *
 * <p>This class references Folia-specific APIs and is only instantiated when
 * {@link PlatformDetector#isFolia()} returns {@code true}. Java's lazy class
 * loading ensures it is never loaded on Paper or Spigot where these APIs are absent.
 */
public final class FoliaTaskScheduler implements TaskScheduler {

    private final Plugin plugin;

    public FoliaTaskScheduler(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(final Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, s -> task.run());
    }

    @Override
    public void runSync(final Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, s -> task.run());
    }

    @Override
    public void runSyncForPlayer(final Player player, final Runnable task) {
        final ScheduledTask result = player.getScheduler().run(plugin, s -> task.run(), null);
        if (result == null) {
            // Player left before the task could be scheduled; run on global region instead.
            Bukkit.getGlobalRegionScheduler().run(plugin, s -> task.run());
        }
    }

    @Override
    public void runSyncLater(final Runnable task, final long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, s -> task.run(), delayTicks);
    }

    @Override
    public CancelableTask scheduleAsyncTimer(
            final Runnable task, final long delayTicks, final long periodTicks) {
        final long delayMs = delayTicks * 50L;
        final long periodMs = periodTicks * 50L;
        final ScheduledTask scheduled = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin, s -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
        return scheduled::cancel;
    }
}
