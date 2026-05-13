package com.pvpindex.factions.engine;

import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.InvitationModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Sends pending invite summaries when players join.
 */
public final class EngineNotifications implements Listener {

    private final InviteService inviteService;
    private final FactionService factionService;
    private final Repositories repos;
    private final Logger logger;
    private final NotificationsConfig notificationsConfig;
    private Plugin plugin;

    public EngineNotifications(
            final InviteService inviteService,
            final FactionService factionService,
            final Repositories repos,
            final Logger logger) {
        this(inviteService, factionService, repos, logger, null);
    }

    public EngineNotifications(
            final InviteService inviteService,
            final FactionService factionService,
            final Repositories repos,
            final Logger logger,
            final NotificationsConfig notificationsConfig) {
        this.inviteService = inviteService;
        this.factionService = factionService;
        this.repos = repos;
        this.logger = logger;
        this.notificationsConfig = notificationsConfig;
    }

    public void register(final Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (plugin == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                final List<InvitationModel> invites =
                    inviteService.listActiveInvitesForPlayer(player.getUniqueId());
                final PlayerModel model = repos.players().findOrCreate(player.getUniqueId().toString());
                if (!invites.isEmpty() && model.hasInviteNotifications()) {
                    Bukkit.getScheduler().runTask(plugin, () -> sendInviteSummary(player, invites));
                }

                final List<com.pvpindex.factions.data.model.FactionInboxEntry> inboxEntries =
                    repos.inbox().findByPlayerId(player.getUniqueId().toString());
                if (!inboxEntries.isEmpty()
                        && (notificationsConfig == null || notificationsConfig.isInboxEnabled())) {
                    repos.inbox().deleteByPlayerId(player.getUniqueId().toString());
                    final int maxEntries = notificationsConfig == null
                        ? 20 : notificationsConfig.getInboxMaxPerLogin();
                    final List<com.pvpindex.factions.data.model.FactionInboxEntry> toDeliver =
                        inboxEntries.size() > maxEntries
                            ? inboxEntries.subList(0, maxEntries) : inboxEntries;
                    Bukkit.getScheduler().runTask(plugin, () -> deliverInbox(player, toDeliver));
                }
            } catch (Exception e) {
                logger.warning("Failed to send join notifications: " + e.getMessage());
            }
        });
    }

    private void sendInviteSummary(final Player player, final List<InvitationModel> invites) {
        MsgUtil.sendKey(
            player,
            "invite.summary",
            "<gold>You have <white>{count}</white> pending faction invite(s):",
            "count",
            Integer.toString(invites.size()));
        for (final InvitationModel invite : invites) {
            final String faction = factionService.getFactionById(invite.getFactionId())
                .map(FactionModel::getName).orElse("Unknown");
            final String inviter = resolvePlayerName(invite.getInviterId());
            MsgUtil.send(player, MsgUtil.inviteListEntry(faction, inviter));
        }
    }

    private String resolvePlayerName(final String uuidStr) {
        try {
            final OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            return player.getName() == null ? uuidStr : player.getName();
        } catch (Exception ignored) {
            return uuidStr == null ? "Unknown" : uuidStr;
        }
    }

    private void deliverInbox(
            final Player player,
            final List<com.pvpindex.factions.data.model.FactionInboxEntry> entries) {
        MsgUtil.sendKey(
            player,
            "notifications.inbox-header",
            "<gold>Missed faction notifications (<white>{count}</white>):",
            "count", Integer.toString(entries.size()));
        for (final com.pvpindex.factions.data.model.FactionInboxEntry entry : entries) {
            MsgUtil.send(player, entry.getMessage());
        }
    }
}
