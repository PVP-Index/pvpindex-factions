package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f demote <player>}. */
public final class CmdDemote extends FactionCommand {

    private final FactionService factionService;

    public CmdDemote(final FactionService factionService) {
        super("demote");
        setPermission("factions.cmd.demote");
        setDescription("Demote a faction member.");
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
        if (factionService.demoteMember(actor.getUniqueId(), target.getUniqueId())) {
            MsgUtil.send(actor, "<yellow>Demoted <white>" + ctx.arg(0) + "<yellow>.");
            return;
        }
        MsgUtil.send(actor, "<red>Could not demote that player.");
    }
}

