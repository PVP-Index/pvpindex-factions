package com.pvpindex.factions.scheduler;

import org.bukkit.entity.Player;

/**
 * Platform-neutral scheduling interface.
 *
 * <p>Use {@link PlatformDetector} to choose between {@link BukkitTaskScheduler}
 * (Paper / Spigot) and {@link FoliaTaskScheduler} (Folia) at startup.
 */
public interface TaskScheduler {

    /**
     * Run a task asynchronously as soon as possible.
     *
     * @param task the runnable to execute
     */
    void runAsync(Runnable task);

    /**
     * Run a task on the primary / global-region thread as soon as possible.
     *
     * @param task the runnable to execute
     */
    void runSync(Runnable task);

    /**
     * Run a task on the thread that owns the given player's region.
     *
     * <p>On Folia this uses the entity scheduler; on Bukkit / Spigot it
     * falls back to the global main thread.
     *
     * @param player the player whose region should own the task
     * @param task   the runnable to execute
     */
    void runSyncForPlayer(Player player, Runnable task);

    /**
     * Run a task on the primary / global-region thread after a delay.
     *
     * @param task       the runnable to execute
     * @param delayTicks delay in server ticks (20 ticks = 1 second)
     */
    void runSyncLater(Runnable task, long delayTicks);

    /**
     * Schedule a repeating task that runs asynchronously.
     *
     * @param task        the runnable to execute on each tick
     * @param delayTicks  initial delay in server ticks before the first run
     * @param periodTicks interval in server ticks between subsequent runs
     * @return a {@link CancelableTask} that can be used to stop the timer
     */
    CancelableTask scheduleAsyncTimer(Runnable task, long delayTicks, long periodTicks);
}
