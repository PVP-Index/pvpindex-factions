package com.pvpindex.factions.integration.essentials;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/** EssentialsX implementation backed by the compile-time EssentialsX API. */
public final class EssentialsXInterop implements EssentialsInterop {

    private final IEssentials essentials;
    private final Logger logger;

    public EssentialsXInterop(final IEssentials essentials, final Logger logger) {
        this.essentials = essentials;
        this.logger = logger;
    }

    @Override
    public boolean teleport(final Player player, final Location destination,
            final Runnable onSuccess, final Runnable onFailure) {
        final User user = essentials.getUser(player);
        if (user == null) {
            return false;
        }
        // Record current position so EssentialsX /back works after this teleport.
        user.setLastLocation();
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        user.getAsyncTeleport().now(
            destination, true, PlayerTeleportEvent.TeleportCause.COMMAND, future);
        future.whenComplete((ok, err) -> {
            if (err != null) {
                logger.warning("EssentialsX async teleport threw: " + err.getMessage());
                onFailure.run();
                return;
            }
            if (Boolean.TRUE.equals(ok)) {
                onSuccess.run();
            } else {
                onFailure.run();
            }
        });
        return true;
    }

    @Override
    public boolean isJailed(final Player player) {
        final User user = essentials.getUser(player);
        return user != null && user.isJailed();
    }

    @Override
    public boolean isVanished(final Player player) {
        final User user = essentials.getUser(player);
        return user != null && user.isVanished();
    }
}

