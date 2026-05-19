package com.pvpindex.factions.integration.worldguard;

import com.pvpindex.factions.config.FactionsConfig;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Creates an optional territory guard integration.
 */
public final class TerritoryGuardFactory {

    private TerritoryGuardFactory() {
    }

    public static TerritoryGuard create(
            final Plugin plugin,
            final FactionsConfig config,
            final Logger logger) {
        if (!config.isWorldGuardEnabled()) {
            logger.info("WorldGuard integration disabled in config.");
            return new NoopTerritoryGuard();
        }
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            logger.info("WorldGuard not found — territory guard disabled.");
            return new NoopTerritoryGuard();
        }
        logger.info("WorldGuard detected — territory guard enabled.");
        return new WorldGuardTerritoryGuard(config.isWorldGuardSyncRegions());
    }
}

