package com.pvpindex.factions.service;

import com.pvpindex.factions.data.model.WarpModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

/**
 * Internal warp service interface — no TeamsAPI dependency.
 */
public interface WarpService {

    /** @return all warps belonging to the given faction. */
    List<WarpModel> getWarps(String factionId);

    /** @return the named warp for the faction, or empty. */
    Optional<WarpModel> getWarp(String factionId, String name);

    /**
     * Create or update the named warp for {@code factionId}.
     *
     * @return {@code true} if the warp was saved (respects warp limit for new warps).
     */
    boolean setWarp(String factionId, String name, Location location, UUID creatorUUID);

    /**
     * Delete the named warp for {@code factionId}.
     *
     * @return {@code true} if the warp was found and deleted.
     */
    boolean deleteWarp(String factionId, String name);
}
