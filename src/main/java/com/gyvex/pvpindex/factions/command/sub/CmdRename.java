package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f rename <name>}. */
public final class CmdRename extends FactionCommand {

    private final FactionService factionService;

    public CmdRename(final FactionService factionService) {
        super("rename");
        setPermission("factions.cmd.rename");
        setDescription("Rename your faction.");
        setRequiredArgs("<name>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (!CommandGuards.requireOwner(player, factionService)) {
            return;
        }
        final String newName = ctx.arg(0);
        if (newName.length() < 3 || newName.length() > 32) {
            MsgUtil.send(player, "<red>Faction name must be between 3 and 32 characters.");
            return;
        }
        if (factionService.renameFaction(player.getUniqueId(), newName)) {
            MsgUtil.send(player, "<green>Faction renamed to <white>" + newName + "<green>.");
            return;
        }
        MsgUtil.send(player, "<red>Could not rename faction (name may already be taken).");
    }
}

