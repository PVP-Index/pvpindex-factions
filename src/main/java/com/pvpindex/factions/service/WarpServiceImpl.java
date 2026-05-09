package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.WarpModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;

/**
 * Core warp business logic — no TeamsAPI dependency.
 */
public class WarpServiceImpl implements WarpService {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public WarpServiceImpl(
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public Repositories getRepos() {
        return repos;
    }

    public FactionsConfig getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    // -------------------------------------------------------------------------
    // WarpService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<WarpModel> getWarps(final String factionId) {
        try {
            return new ArrayList<>(repos.warps().findByFactionId(factionId));
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get warps for faction " + factionId, e);
            return List.of();
        }
    }

    @Override
    public Optional<WarpModel> getWarp(final String factionId, final String name) {
        try {
            return repos.warps().findByFactionIdAndName(factionId, name);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get warp '" + name + "' for faction " + factionId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean setWarp(
            final String factionId,
            final String name,
            final Location location,
            final UUID creatorUUID) {
        try {
            final Optional<FactionModel> faction = repos.factions().find(factionId);
            if (faction.isEmpty() || location.getWorld() == null) {
                return false;
            }

            final int currentCount = repos.warps().findByFactionId(factionId).size();
            final Optional<WarpModel> existing =
                repos.warps().findByFactionIdAndName(factionId, name);
            if (existing.isEmpty() && currentCount >= config.getMaxWarps()) {
                return false;
            }

            final WarpModel warpModel = existing.orElseGet(() ->
                new WarpModel(UUID.randomUUID().toString()));
            warpModel.setFactionId(factionId);
            warpModel.setName(name);
            warpModel.setWorld(location.getWorld().getName());
            warpModel.setX(location.getX());
            warpModel.setY(location.getY());
            warpModel.setZ(location.getZ());
            warpModel.setYaw(location.getYaw());
            warpModel.setPitch(location.getPitch());
            if (creatorUUID != null) {
                warpModel.setCreatorId(creatorUUID.toString());
            }
            if (existing.isEmpty()) {
                warpModel.setCreatedAt(System.currentTimeMillis());
            }

            repos.warps().save(warpModel);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to set warp '" + name + "' for faction " + factionId, e);
            return false;
        }
    }

    @Override
    public boolean deleteWarp(final String factionId, final String name) {
        try {
            final Optional<WarpModel> warp =
                repos.warps().findByFactionIdAndName(factionId, name);
            if (warp.isEmpty()) {
                return false;
            }
            repos.warps().delete(warp.get().getId());
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to delete warp '" + name + "' for faction " + factionId, e);
            return false;
        }
    }
}
