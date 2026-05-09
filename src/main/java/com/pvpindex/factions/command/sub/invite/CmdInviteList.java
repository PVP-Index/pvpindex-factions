package com.pvpindex.factions.command.sub.invite;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.InvitationModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f invite list [faction]}. */
public final class CmdInviteList extends FactionCommand {

    private final FactionService factionService;
    private final InviteService inviteService;

    public CmdInviteList(final FactionService factionService, final InviteService inviteService) {
        super("list");
        setPermission("factions.cmd.invite.list");
        setDescription("List pending faction invites.");
        setOptionalArgs("[faction]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.inviteService = inviteService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (ctx.getArgs().isEmpty()) {
            final List<InvitationModel> invites = inviteService.listActiveInvitesForPlayer(player.getUniqueId());
            if (invites.isEmpty()) {
                MsgUtil.send(player, "<yellow>You have no pending invites.");
                return;
            }
            MsgUtil.send(player, "<gold>You have <white>" + invites.size() + "<gold> pending faction invite(s):");
            for (final InvitationModel invite : invites) {
                final String name = factionService.getFactionById(invite.getFactionId())
                    .map(FactionModel::getName)
                    .orElse("Unknown");
                final String inviter = resolveInviter(invite);
                player.sendMessage(MsgUtil.inviteListEntry(name, inviter));
            }
            return;
        }
        if (!player.hasPermission("factions.admin")) {
            MsgUtil.send(player, "<red>You do not have permission to list another faction's invites.");
            return;
        }
        final Optional<FactionModel> faction = factionService.getFactionByName(ctx.arg(0));
        if (faction.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        final List<InvitationModel> invites = inviteService.listInvitesForFaction(faction.get().getId());
        if (invites.isEmpty()) {
            MsgUtil.send(player, "<yellow>No pending invites for <white>" + faction.get().getName() + "<yellow>.");
            return;
        }
        MsgUtil.send(player, "<gold>== Pending Invites: <white>" + faction.get().getName() + "<gold> ==");
        for (final InvitationModel invite : invites) {
            final OfflinePlayer invitee = Bukkit.getOfflinePlayer(java.util.UUID.fromString(invite.getInviteeId()));
            MsgUtil.send(player, "<yellow>- <white>" + (invitee.getName() == null ? invite.getInviteeId() : invitee.getName()));
        }
    }

    private String resolveInviter(final InvitationModel invite) {
        try {
            final OfflinePlayer inviter = Bukkit.getOfflinePlayer(UUID.fromString(invite.getInviterId()));
            return inviter.getName() == null ? invite.getInviterId() : inviter.getName();
        } catch (Exception ignored) {
            return invite.getInviterId();
        }
    }
}
