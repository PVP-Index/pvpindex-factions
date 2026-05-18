package com.pvpindex.factions.integration.essentials;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Optional bridge for EssentialsX behavior. */
public interface EssentialsInterop {

    /**
     * Attempt an async teleport through EssentialsX.
     *
     * <p>When the interop handles the request it returns {@code true} and fires exactly
     * one of {@code onSuccess} or {@code onFailure} asynchronously on completion.
     * When it returns {@code false} the caller must fall back to a native teleport.
     */
    boolean teleport(Player player, Location destination, Runnable onSuccess, Runnable onFailure);

    /** Returns {@code true} when EssentialsX reports this player as jailed. */
    boolean isJailed(Player player);

    /** Returns {@code true} when EssentialsX reports this player as admin-vanished. */
    boolean isVanished(Player player);
}

