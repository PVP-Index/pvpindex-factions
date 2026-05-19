package com.pvpindex.factions.integration.essentials;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Disabled/default interop that never handles teleports or state checks. */
public final class NoopEssentialsInterop implements EssentialsInterop {

    @Override
    public boolean teleport(final Player player, final Location destination,
            final Runnable onSuccess, final Runnable onFailure) {
        return false;
    }

    @Override
    public boolean isJailed(final Player player) {
        return false;
    }

    @Override
    public boolean isVanished(final Player player) {
        return false;
    }
}

