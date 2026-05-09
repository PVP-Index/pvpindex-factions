package com.pvpindex.factions.command.sub.invite;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f invite revoke <player>}. */
public final class CmdInviteRevoke extends FactionCommand {

    private final FactionService factionService;
    private final InviteService inviteService;

    public CmdInviteRevoke(final FactionService factionService, final InviteService inviteService) {
        super("revoke");
        setPermission("factions.cmd.invite.revoke");
        setDescription("Revoke a pending invite.");
        setRequiredArgs("<player>");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.inviteService = inviteService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        final Optional<FactionModel> faction = CommandGuards.requireFaction(actor, factionService);
        if (faction.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(ctx.arg(0));
        if (target.getUniqueId() == null) {
            MsgUtil.send(actor, "<red>Player not found.");
            return;
        }
        if (inviteService.revokeInvite(faction.get().getId(), target.getUniqueId())) {
            MsgUtil.send(actor, "<yellow>Invite revoked for <white>" + ctx.arg(0) + "<yellow>.");
            return;
        }
        MsgUtil.send(actor, "<red>No pending invite for that player.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex != 0 || !ctx.isPlayer()) {
            return List.of();
        }
        final Optional<FactionModel> faction = factionService.getFactionByPlayer(((Player) ctx.getSender()).getUniqueId());
        if (faction.isEmpty()) {
            return List.of();
        }
        return inviteService.listInvitesForFaction(faction.get().getId()).stream()
            .map(i -> Bukkit.getOfflinePlayer(java.util.UUID.fromString(i.getInviteeId())))
            .map(OfflinePlayer::getName)
            .filter(n -> n != null && !n.isBlank())
            .toList();
    }
}

