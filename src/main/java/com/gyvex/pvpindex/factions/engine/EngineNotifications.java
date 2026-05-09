package com.gyvex.pvpindex.factions.engine;

import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.InvitationModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.service.InviteService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
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
    private Plugin plugin;

    public EngineNotifications(
            final InviteService inviteService,
            final FactionService factionService,
            final Repositories repos,
            final Logger logger) {
        this.inviteService = inviteService;
        this.factionService = factionService;
        this.repos = repos;
        this.logger = logger;
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
                if (invites.isEmpty()) {
                    return;
                }
                final PlayerModel model = repos.players().findOrCreate(player.getUniqueId().toString());
                if (!model.hasInviteNotifications()) {
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> sendInviteSummary(player, invites));
            } catch (Exception e) {
                logger.warning("Failed to send invite notifications: " + e.getMessage());
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
}
