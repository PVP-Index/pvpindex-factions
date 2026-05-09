package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f unsethome [confirm]}. */
public final class CmdUnsetHome extends FactionCommand {

    private final FactionService factionService;

    public CmdUnsetHome(final FactionService factionService) {
        super("unsethome");
        setPermission("factions.cmd.sethome");
        setDescription("Unset faction home.");
        setOptionalArgs("[confirm]");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        if (!"confirm".equalsIgnoreCase(ctx.arg(0))) {
            MsgUtil.send(player, "<red>Use /f unsethome confirm to remove faction home.");
            return;
        }
        if (factionService.unsetFactionHome(player.getUniqueId())) {
            MsgUtil.send(player, "<yellow>Faction home removed.");
            return;
        }
        MsgUtil.send(player, "<red>Failed to remove faction home.");
    }
}

