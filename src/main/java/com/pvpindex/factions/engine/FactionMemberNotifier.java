package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PlayerModel;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Reusable helper for notifying online members of a faction.
 */
public final class FactionMemberNotifier {

    private FactionMemberNotifier() {
    }

    public static void notifyOnlineMembers(
            final Plugin plugin,
            final Repositories repos,
            final Logger logger,
            final String factionId,
            final Predicate<PlayerModel> filter,
            final Consumer<Player> notifyAction) {
        if (repos == null || repos.players() == null) {
            return;
        }
        try {
            final List<PlayerModel> members = repos.players().findByFactionId(factionId);
            for (final PlayerModel member : members) {
                if (filter != null && !filter.test(member)) {
                    continue;
                }
                final UUID memberUuid;
                try {
                    memberUuid = UUID.fromString(member.getId());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                final Player online = Bukkit.getPlayer(memberUuid);
                if (online == null || !online.isOnline()) {
                    continue;
                }
                if (plugin == null) {
                    notifyAction.accept(online);
                } else {
                    final BukkitScheduler scheduler = Bukkit.getScheduler();
                    if (scheduler == null) {
                        notifyAction.accept(online);
                    } else {
                        scheduler.runTask(plugin, () -> notifyAction.accept(online));
                    }
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to notify faction members for faction " + factionId, e);
        }
    }
}
