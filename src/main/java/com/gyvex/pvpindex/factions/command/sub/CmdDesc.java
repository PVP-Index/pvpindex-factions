package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f desc <text...>}. */
public final class CmdDesc extends FactionCommand {

    private final FactionService factionService;

    public CmdDesc(final FactionService factionService) {
        super("desc");
        setPermission("factions.cmd.desc");
        setDescription("Set faction description.");
        setRequiredArgs("<text...>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (!CommandGuards.requireOwner(player, factionService)) {
            return;
        }
        final String description = String.join(" ", ctx.getArgs()).trim();
        if (description.length() > 250) {
            MsgUtil.send(player, "<red>Description is too long (max 250 chars).");
            return;
        }
        if (factionService.setFactionDescription(player.getUniqueId(), description)) {
            MsgUtil.send(player, "<green>Faction description updated.");
            return;
        }
        MsgUtil.send(player, "<red>Could not update faction description.");
    }
}

