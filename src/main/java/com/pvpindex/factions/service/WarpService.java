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

    /**
     * Set or clear the password for a named warp.
     *
     * @param password new password, or {@code null}/{@code ""} to clear
     * @return {@code true} if the warp was found and updated
     */
    boolean setWarpPassword(String factionId, String name, String password);

    /**
     * Set the per-use economy cost for a named warp.
     *
     * @param cost amount ≥ 0; pass 0 to make the warp free
     * @return {@code true} if the warp was found and updated
     */
    boolean setWarpCost(String factionId, String name, double cost);
}
