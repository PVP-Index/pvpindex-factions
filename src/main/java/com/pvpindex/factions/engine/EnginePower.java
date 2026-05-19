package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.scheduler.CancelableTask;
import com.pvpindex.factions.scheduler.TaskScheduler;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Periodically ticks player power and handles power updates on login/logout.
 */
public final class EnginePower implements Runnable, Listener {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;
    private final TaskScheduler taskScheduler;
    private CancelableTask timerTask;
    private final long startedAt = System.currentTimeMillis();

    public EnginePower(
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger,
            final TaskScheduler taskScheduler) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Schedule this engine to run every {@code powerTickIntervalSeconds} seconds
     * and register Bukkit event listeners.
     *
     * @param plugin owning plugin (used only for listener registration)
     */
    public void start(final Plugin plugin) {
        final int intervalSeconds = Math.max(1, config.getPowerTickIntervalSeconds());
        final long intervalTicks = intervalSeconds * 20L;
        timerTask = taskScheduler.scheduleAsyncTimer(this, intervalTicks, intervalTicks);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Cancel the periodic power tick. Call from engine shutdown.
     */
    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    @Override
    public void run() {
        try {
            final List<PlayerModel> players = repos.players().findAll();
            for (final PlayerModel pm : players) {
                tickPower(pm);
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to tick power for players", e);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        taskScheduler.runAsync(
            () -> applyPowerOnQuit(event.getPlayer().getUniqueId().toString()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        final Player dead = event.getPlayer();
        final Player killer = dead.getKiller();
        final String deadId = dead.getUniqueId().toString();
        final String killerId = killer != null ? killer.getUniqueId().toString() : null;
        final String worldName = dead.getWorld().getName();
        final int chunkX = dead.getLocation().getChunk().getX();
        final int chunkZ = dead.getLocation().getChunk().getZ();
        taskScheduler.runAsync(
            () -> applyDeathPower(deadId, killerId, worldName, chunkX, chunkZ));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void tickPower(final PlayerModel pm) throws StorageException {
        final double current = pm.getPower();
        final double max = config.getMaxPower();
        final Player online = Bukkit.getPlayer(java.util.UUID.fromString(pm.getId()));

        double regen;
        if (online != null) {
            regen = config.getPowerRegenOnline();
        } else {
            regen = config.getPowerRegenOffline();
        }

        if (current < max) {
            pm.setPower(Math.min(max, current + regen));
            repos.players().save(pm);
        }
    }

    private void applyPowerOnQuit(final String playerId) {
        // Power loss on death is handled by onDeath; nothing extra here.
    }

    private void applyDeathPower(
            final String deadId,
            final String killerId,
            final String worldName,
            final int chunkX,
            final int chunkZ) {
        try {
            // Respect server start grace period — no power loss during grace window.
            if (System.currentTimeMillis() - startedAt < config.getPowerGracePeriodSeconds() * 1000L) {
                return;
            }

            // Skip power changes in safezone territory when safe zones are active.
            final Optional<BoardEntry> claim = repos.board().findByChunk(worldName, chunkX, chunkZ);
            if (config.isSafeZoneEnabled()
                    && claim.isPresent()
                    && FactionModel.SAFEZONE_ID.equals(claim.get().getFactionId())) {
                return;
            }

            // Apply power loss to dead player.
            final Optional<PlayerModel> deadOpt = repos.players().find(deadId);
            if (deadOpt.isPresent()) {
                final PlayerModel deadModel = deadOpt.get();
                final double loss = config.getPowerLossOnDeath();
                final double newPower = Math.max(0.0, deadModel.getPower() - loss);
                deadModel.setPower(newPower);
                repos.players().save(deadModel);
                final String lossMsg = MsgUtil.replace(
                    MsgUtil.message("power.lost-on-death", "<red>You lost <yellow>{amount}<red> power on death."),
                    "amount", String.format(java.util.Locale.ROOT, "%.1f", loss));
                taskScheduler.runSync(() -> {
                    final Player deadPlayer = Bukkit.getPlayer(UUID.fromString(deadId));
                    if (deadPlayer != null) {
                        MsgUtil.send(deadPlayer, lossMsg);
                    }
                });
            }

            // Apply power gain to the killer if feature is enabled.
            if (killerId != null && config.isPowerGainOnKillEnabled()) {
                final Optional<PlayerModel> killerOpt = repos.players().find(killerId);
                if (killerOpt.isPresent()) {
                    final PlayerModel killerModel = killerOpt.get();
                    final double gain = config.getPowerGainOnKill();
                    final double maxPower = config.getMaxPower();
                    final double newPower = Math.min(maxPower, killerModel.getPower() + gain);
                    killerModel.setPower(newPower);
                    repos.players().save(killerModel);
                    final String gainMsg = MsgUtil.replace(
                        MsgUtil.message("power.kill-gained", "<green>You gained <yellow>{amount}<green> power from your kill."),
                        "amount", String.format(java.util.Locale.ROOT, "%.1f", gain));
                    taskScheduler.runSync(() -> {
                        final Player killerPlayer = Bukkit.getPlayer(UUID.fromString(killerId));
                        if (killerPlayer != null) {
                            MsgUtil.send(killerPlayer, gainMsg);
                        }
                    });
                }
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to apply death power changes", e);
        }
    }

    /**
     * Compute the total power for a faction from the DB (sum of member power + power boost).
     *
     * @param factionId faction ID
     * @return total power
     */
    public double computeTotalPower(final String factionId) throws StorageException {
        final Optional<FactionModel> faction = repos.factions().find(factionId);
        double total = faction.map(FactionModel::getPowerBoost).orElse(0.0);
        for (final PlayerModel pm : repos.players().findByFactionId(factionId)) {
            total += pm.getPower();
        }
        return total;
    }
}
