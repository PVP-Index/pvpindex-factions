package com.pvpindex.factions.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * {@link TaskScheduler} implementation for Paper and Spigot.
 * Delegates to {@code Bukkit.getScheduler()}.
 */
public final class BukkitTaskScheduler implements TaskScheduler {

    private final Plugin plugin;

    public BukkitTaskScheduler(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(final Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runSync(final Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runSyncForPlayer(final Player player, final Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runSyncLater(final Runnable task, final long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public CancelableTask scheduleAsyncTimer(
            final Runnable task, final long delayTicks, final long periodTicks) {
        final BukkitTask bukkit =
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkit::cancel;
    }
}
