package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically ticks player power and handles power updates on login/logout.
 */
public final class EnginePower extends BukkitRunnable implements Listener {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public EnginePower(
            final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    /**
     * Schedule this engine to run every 60 seconds (1200 ticks).
     *
     * @param plugin owning plugin
     */
    public void start(final Plugin plugin) {
        final int intervalSeconds = Math.max(1, config.getPowerTickIntervalSeconds());
        final long intervalTicks = intervalSeconds * 20L;
        runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("PvPIndexFactions"),
            () -> applyPowerOnQuit(event.getPlayer().getUniqueId().toString()));
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
        // Power loss on death is handled by EnginePlayerMove; nothing extra here.
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
