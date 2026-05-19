package com.pvpindex.factions.integration.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Optional territory permission guard.
 */
public interface TerritoryGuard {

    /**
     * Returns true when the player is allowed to modify/claim territory at location.
     */
    boolean canModifyTerritory(Player player, Location location);

    /**
     * Returns true when this guard is syncing faction claim chunks as WorldGuard regions,
     * allowing WG to handle build protection natively without per-event database queries.
     */
    default boolean syncsBuildProtection() {
        return false;
    }

    /**
     * Returns true when the given location falls inside a WG region that was created by
     * this guard's faction-claim sync. Used by the protection engine to distinguish
     * faction territory (handled by WG) from wilderness (no region).
     */
    default boolean isFactionRegion(Location location) {
        return false;
    }
}

