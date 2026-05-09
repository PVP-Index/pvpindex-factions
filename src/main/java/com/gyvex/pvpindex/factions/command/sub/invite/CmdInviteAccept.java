package com.gyvex.pvpindex.factions.command.sub.invite;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.service.InviteService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f invite accept <faction>} */
public final class CmdInviteAccept extends FactionCommand {

    private final InviteService inviteService;
    private final FactionService factionService;

    public CmdInviteAccept(final InviteService inviteService, final FactionService factionService) {
        super("accept");
        setPermission("factions.cmd.join");
        setDescription("Accept a faction invite.");
        setRequiredArgs("<faction>");
        setRequiresPlayer(true);
        this.inviteService = inviteService;
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (factionService.isInFaction(player.getUniqueId())) {
            MsgUtil.send(player, "<red>You are already in a faction.");
            return;
        }
        final Optional<FactionModel> faction = factionService.getFactionByName(ctx.arg(0));
        if (faction.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        final Optional<FactionModel> joined = inviteService.acceptInvite(faction.get().getId(), player.getUniqueId());
        if (joined.isPresent()) {
            MsgUtil.send(player, "<green>You joined <white>" + joined.get().getName() + "<green>!");
            return;
        }
        MsgUtil.send(player, "<red>You do not have a pending invite from that faction.");
    }
}

