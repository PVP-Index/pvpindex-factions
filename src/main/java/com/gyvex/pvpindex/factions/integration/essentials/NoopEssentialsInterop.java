package com.gyvex.pvpindex.factions.integration.essentials;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Disabled/default interop that never handles teleports. */
public final class NoopEssentialsInterop implements EssentialsInterop {

    @Override
    public boolean teleportToFactionHome(final Player player, final Location home) {
        return false;
    }
}

