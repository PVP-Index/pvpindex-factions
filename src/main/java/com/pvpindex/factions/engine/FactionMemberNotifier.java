package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionInboxEntry;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.scheduler.TaskScheduler;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Reusable helper for notifying online members of a faction.
 */
public final class FactionMemberNotifier {

    private FactionMemberNotifier() {
    }

    public static void notifyOnlineMembers(
            final TaskScheduler scheduler,
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
                if (scheduler == null) {
                    notifyAction.accept(online);
                } else {
                    scheduler.runSyncForPlayer(online, () -> notifyAction.accept(online));
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to notify faction members for faction " + factionId, e);
        }
    }

    /**
     * Notify all members of a faction with a resolved MiniMessage string.
     *
     * <p>Online members receive the message immediately on the appropriate thread.
     * Offline members have the message persisted to their inbox for delivery
     * when they next join.
     *
     * @param scheduler task scheduler (may be {@code null} in tests — runs inline)
     * @param repos     repository container — must have a non-null {@code inbox()}
     * @param logger    logger for error reporting
     * @param factionId faction UUID string
     * @param filter    optional predicate; members that do not pass are skipped entirely
     * @param message   resolved MiniMessage string to deliver
     */
    public static void notifyMembers(
            final TaskScheduler scheduler,
            final Repositories repos,
            final Logger logger,
            final String factionId,
            final Predicate<PlayerModel> filter,
            final String message) {
        if (repos == null || repos.players() == null) {
            return;
        }
        final List<PlayerModel> members;
        try {
            members = repos.players().findByFactionId(factionId);
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to load faction members for inbox notification: " + factionId, e);
            return;
        }
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
            if (online != null && online.isOnline()) {
                if (scheduler == null) {
                    MsgUtil.send(online, message);
                } else {
                    scheduler.runSyncForPlayer(online, () -> MsgUtil.send(online, message));
                }
            } else {
                // Queue for offline delivery — run async so we don't block the caller.
                final String playerId = member.getId();
                final Runnable persist = () -> {
                    try {
                        final FactionInboxEntry entry = new FactionInboxEntry(UUID.randomUUID().toString());
                        entry.setPlayerId(playerId);
                        entry.setMessage(message);
                        entry.setCreatedAt(System.currentTimeMillis());
                        repos.inbox().save(entry);
                    } catch (StorageException ex) {
                        logger.log(Level.WARNING, "Failed to queue inbox entry for player " + playerId, ex);
                    }
                };
                if (scheduler == null) {
                    persist.run();
                } else {
                    scheduler.runAsync(persist);
                }
            }
        }
    }
}
