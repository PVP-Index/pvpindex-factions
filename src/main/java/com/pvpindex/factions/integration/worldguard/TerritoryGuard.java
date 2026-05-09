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
}

