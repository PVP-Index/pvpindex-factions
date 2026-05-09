package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f promote <player>}. */
public final class CmdPromote extends FactionCommand {

    private final FactionService factionService;

    public CmdPromote(final FactionService factionService) {
        super("promote");
        setPermission("factions.cmd.promote");
        setDescription("Promote a faction member.");
        setRequiredArgs("<player>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(ctx.arg(0));
        if (factionService.promoteMember(actor.getUniqueId(), target.getUniqueId())) {
            MsgUtil.send(actor, "<green>Promoted <white>" + ctx.arg(0) + "<green>.");
            return;
        }
        MsgUtil.send(actor, "<red>Could not promote that player.");
    }
}

