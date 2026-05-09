package com.pvpindex.factions.command.sub.invite;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f invite declineall}. */
public final class CmdInviteDeclineAll extends FactionCommand {

    private final InviteService inviteService;

    public CmdInviteDeclineAll(final InviteService inviteService) {
        super("declineall");
        setPermission("factions.cmd.invite");
        setDescription("Decline all pending invites.");
        setRequiresPlayer(true);
        this.inviteService = inviteService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final int removed = inviteService.declineAllInvites(player.getUniqueId());
        MsgUtil.send(player, removed > 0
            ? "<yellow>Declined <white>" + removed + "<yellow> invite(s)."
            : "<yellow>You have no pending invites.");
    }
}

