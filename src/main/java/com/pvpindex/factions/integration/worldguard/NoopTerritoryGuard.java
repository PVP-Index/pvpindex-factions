package com.pvpindex.factions.integration.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Default guard when WorldGuard integration is disabled/unavailable.
 */
public final class NoopTerritoryGuard implements TerritoryGuard {

    @Override
    public boolean canModifyTerritory(final Player player, final Location location) {
        return true;
    }
}

