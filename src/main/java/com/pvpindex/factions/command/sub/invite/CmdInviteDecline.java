package com.pvpindex.factions.command.sub.invite;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f invite decline <faction>}. */
public final class CmdInviteDecline extends FactionCommand {

    private final InviteService inviteService;
    private final FactionService factionService;

    public CmdInviteDecline(final InviteService inviteService, final FactionService factionService) {
        super("decline");
        setPermission("factions.cmd.invite");
        setDescription("Decline a faction invite.");
        setRequiredArgs("<faction>");
        setRequiresPlayer(true);
        this.inviteService = inviteService;
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> faction = factionService.getFactionByName(ctx.arg(0));
        if (faction.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        if (inviteService.declineInvite(faction.get().getId(), player.getUniqueId())) {
            MsgUtil.send(player, "<yellow>Invite declined.");
            return;
        }
        MsgUtil.send(player, "<red>You do not have a pending invite from that faction.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex != 0 || !ctx.isPlayer()) {
            return List.of();
        }
        final Player player = (Player) ctx.getSender();
        return inviteService.listInvitesForPlayer(player.getUniqueId()).stream()
            .map(i -> factionService.getFactionById(i.getFactionId()).map(FactionModel::getName).orElse(null))
            .filter(n -> n != null && !n.isBlank())
            .toList();
    }
}

