package com.gyvex.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.AutoTerritoryMode;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runtime cache for persisted auto-territory preferences.
 */
public class AutoTerritoryModeCache {

    private final Repositories repos;
    private final Logger logger;
    private final Map<UUID, AutoTerritoryMode> cache = new ConcurrentHashMap<>();

    public AutoTerritoryModeCache(final Repositories repos, final Logger logger) {
        this.repos = repos;
        this.logger = logger;
    }

    public AutoTerritoryMode getMode(final UUID playerId) {
        return cache.getOrDefault(playerId, AutoTerritoryMode.OFF);
    }

    public boolean setMode(final UUID playerId, final AutoTerritoryMode mode) {
        try {
            final PlayerModel model = repos.players().findOrCreate(playerId.toString());
            model.setAutoTerritoryMode(mode);
            repos.players().save(model);
            cache.put(playerId, mode == null ? AutoTerritoryMode.OFF : mode);
            return true;
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to persist auto territory mode for " + playerId, e);
            return false;
        }
    }

    public AutoTerritoryMode hydrate(final UUID playerId) {
        try {
            final PlayerModel model = repos.players().findOrCreate(playerId.toString());
            final AutoTerritoryMode mode = model.getAutoTerritoryMode();
            cache.put(playerId, mode);
            return mode;
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to hydrate auto territory mode for " + playerId, e);
            cache.put(playerId, AutoTerritoryMode.OFF);
            return AutoTerritoryMode.OFF;
        }
    }

    public void evict(final UUID playerId) {
        cache.remove(playerId);
    }
}
