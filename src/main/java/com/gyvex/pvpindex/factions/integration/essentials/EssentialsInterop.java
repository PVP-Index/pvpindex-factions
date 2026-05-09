package com.gyvex.pvpindex.factions.integration.essentials;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Optional bridge for Essentials-style teleport behavior. */
public interface EssentialsInterop {

    /**
     * Attempt teleport via Essentials integration.
     *
     * @return {@code true} when interop handled the request, otherwise {@code false}
     */
    boolean teleportToFactionHome(Player player, Location home);
}

