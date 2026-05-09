package com.pvpindex.factions.integration.essentials;

import com.pvpindex.factions.util.MsgUtil;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

/** EssentialsX implementation that uses async teleport API via reflection. */
public final class EssentialsXInterop implements EssentialsInterop {

    private final Plugin essentialsPlugin;
    private final Logger logger;

    public EssentialsXInterop(final Plugin essentialsPlugin, final Logger logger) {
        this.essentialsPlugin = essentialsPlugin;
        this.logger = logger;
    }

    @Override
    public boolean teleportToFactionHome(final Player player, final Location home) {
        try {
            final Method getUser = essentialsPlugin.getClass().getMethod("getUser", Player.class);
            final Object user = getUser.invoke(essentialsPlugin, player);
            if (user == null) {
                return false;
            }

            final Method getAsyncTeleport = user.getClass().getMethod("getAsyncTeleport");
            final Object asyncTeleport = getAsyncTeleport.invoke(user);
            if (asyncTeleport == null) {
                return false;
            }

            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            final Method now = asyncTeleport.getClass().getMethod(
                "now",
                Location.class,
                boolean.class,
                PlayerTeleportEvent.TeleportCause.class,
                CompletableFuture.class
            );
            now.invoke(asyncTeleport, home, true, PlayerTeleportEvent.TeleportCause.COMMAND, future);
            future.whenComplete((ok, err) -> {
                if (err != null || !Boolean.TRUE.equals(ok)) {
                    MsgUtil.send(player, "<red>Faction home teleport failed.");
                    return;
                }
                MsgUtil.send(player, "<green>Teleported to faction home.");
            });
            return true;
        } catch (ReflectiveOperationException e) {
            logger.log(Level.FINE, "EssentialsX interop unavailable, falling back to native teleport.", e);
            return false;
        }
    }
}

