package com.pvpindex.factions.command.sub;

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

/** {@code /f join <factionName>} — Accept a pending invite and join a faction. */
public final class CmdJoin extends FactionCommand {

    private final FactionService factionService;
    private final InviteService inviteService;

    public CmdJoin(
            final FactionService factionService,
            final InviteService inviteService) {
        super("join");
        setPermission("factions.cmd.join");
        setDescription("Accept an invite and join a faction.");
        setOptionalArgs("[factionName]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.inviteService = inviteService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (factionService.isInFaction(player.getUniqueId())) {
            MsgUtil.send(player, "<red>You are already in a faction.");
            return;
        }
        if (ctx.getArgs().isEmpty()) {
            final List<InvitationModel> invites = inviteService.listActiveInvitesForPlayer(player.getUniqueId());
            if (invites.isEmpty()) {
                MsgUtil.send(player, "<yellow>You have no pending invites.");
                return;
            }
            MsgUtil.send(player, "<gold>You have <white>" + invites.size() + "<gold> pending faction invite(s):");
            for (final InvitationModel invite : invites) {
                final String factionName = factionService.getFactionById(invite.getFactionId())
                    .map(FactionModel::getName).orElse("Unknown");
                MsgUtil.send(player, MsgUtil.inviteListEntry(factionName, resolveInviter(invite)));
            }
            return;
        }
        final Optional<FactionModel> factionOpt = factionService.getFactionByName(ctx.arg(0));
        if (factionOpt.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        final Optional<FactionModel> joined = inviteService.acceptInvite(
            factionOpt.get().getId(), player.getUniqueId());
        if (joined.isPresent()) {
            MsgUtil.send(player, "<green>You joined <white>" + joined.get().getName() + "<green>!");
        } else {
            MsgUtil.send(player, "<red>You do not have a pending invite from that faction.");
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

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return factionService.getAllFactions().stream()
                .map(FactionModel::getName)
                .toList();
        }
        return List.of();
    }
}
