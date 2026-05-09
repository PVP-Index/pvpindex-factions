package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f leader <player> [confirm]}. */
public final class CmdLeader extends FactionCommand {

    private final FactionService factionService;

    public CmdLeader(final FactionService factionService) {
        super("leader");
        setPermission("factions.cmd.leader");
        setDescription("Transfer faction ownership to another member.");
        setRequiredArgs("<player>");
        setOptionalArgs("[confirm]");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player owner = (Player) ctx.getSender();
        if (!CommandGuards.requireOwner(owner, factionService)) {
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(ctx.arg(0));
        if (target.getUniqueId().equals(owner.getUniqueId()) && !"confirm".equalsIgnoreCase(ctx.arg(1))) {
            MsgUtil.send(owner, "<red>Use /f leader " + owner.getName() + " confirm to re-affirm self leadership.");
            return;
        }
        if (factionService.transferOwnership(owner.getUniqueId(), target.getUniqueId())) {
            MsgUtil.send(owner, "<green>Faction ownership transferred to <white>" + ctx.arg(0) + "<green>.");
            return;
        }
        MsgUtil.send(owner, "<red>Could not transfer ownership.");
    }
}

